package com.fugary.simple.mock.script;

import com.fugary.simple.mock.cache.MockCacheNames;
import com.fugary.simple.mock.cache.RequireSourceCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RequireCacheTest {
    private final AtomicLong clock = new AtomicLong();
    private final RequireSourceCache cache = createCache();

    private RequireSourceCache createCache() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(MockCacheNames.REQUIRE_SOURCE, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5)).ticker(clock::get).build());
        return new RequireSourceCache(manager);
    }

    private Context engine() throws Exception {
        Context context = Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false").build();
        context.getBindings("js").putMember("__requireSourceCache__", cache);
        context.getBindings("js").putMember("__requireCache__", ProxyObject.fromMap(new HashMap<>()));
        context.eval("js", "var calls = 0; var executions = 0;"
                + "var script = 'executions++; module.exports = {value: 1, fn: () => 42}';"
                + "var ok = true; var fetch = () => { calls++; return Promise.resolve({"
                + "ok, status: ok ? 200 : 500, text: () => script }); };");
        try (java.io.InputStream stream = getClass().getResourceAsStream("/js/simple-mock.js")) {
            context.eval("js", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        return context;
    }

    private int require(Context context, String options) {
        context.eval("js", "var result; var error; require('https://example.com/lib.js'" + options
                + ").then(v => { result = typeof v === 'object' ? v.value : v; }, e => { error = String(e); });");
        assertTrue(context.getBindings("js").getMember("error").isNull());
        return context.getBindings("js").getMember("result").asInt();
    }

    @Test
    void expiresAcrossEnginesWithoutExtendingOnReads() throws Exception {
        try (Context first = engine(); Context second = engine()) {
            assertEquals(1, require(first, ""));
            assertEquals(1, require(second, ""));
            assertEquals(0, second.eval("js", "calls").asInt());
            assertEquals(42, second.eval("js", "__requireCache__['https://example.com/lib.js'].exports.fn()").asInt());
            clock.set(Duration.ofMinutes(4).toNanos());
            assertEquals(1, require(first, ""));
            assertEquals(1, first.eval("js", "executions").asInt());
            first.eval("js", "script = 'executions++; module.exports = {value: 2}';");
            clock.set(Duration.ofMinutes(5).toNanos());
            assertEquals(2, require(first, ""));
            assertEquals(2, require(second, ""));
            assertEquals(2, second.eval("js", "executions").asInt());
            assertEquals(2, first.eval("js", "calls").asInt());
            assertEquals(0, second.eval("js", "calls").asInt());
        }
    }

    @Test
    void bypassDoesNotReplaceCacheAndClearInvalidatesOtherEngines() throws Exception {
        try (Context first = engine(); Context second = engine()) {
            assertEquals(1, require(first, ""));
            assertEquals(1, require(second, ""));
            first.eval("js", "script = 'module.exports = 2';");
            assertEquals(2, require(first, ", {cache: false}"));
            assertEquals(1, require(first, ""));
            first.eval("js", "clearRequireCache();");
            second.eval("js", "script = 'executions++; module.exports = 0';");
            assertEquals(0, require(second, ""));
            assertEquals(0, require(second, ""));
            assertEquals(2, second.eval("js", "executions").asInt());
            assertEquals(0, require(first, ""));
        }
    }

    @Test
    void failedLoadsAreNotCachedAndCanBeRetried() throws Exception {
        try (Context context = engine()) {
            context.eval("js", "ok = false; require('https://example.com/lib.js').catch(() => {});");
            assertNull(cache.get("https://example.com/lib.js"));
            context.eval("js", "ok = true; script = 'throw new Error(\"bad script\")';"
                    + "require('https://example.com/lib.js').catch(() => {});");
            assertNull(cache.get("https://example.com/lib.js"));
            context.eval("js", "script = 'module.exports = 3';");
            assertEquals(3, require(context, ""));
            assertEquals(3, context.eval("js", "calls").asInt());
        }
    }

    @Test
    void clearPreventsAnEarlierLoadFromRepopulatingCache() {
        String generation = cache.generation();
        cache.clear();
        assertNull(cache.getOrPut("url", "old script", generation));
        assertNull(cache.get("url"));
        assertNotNull(cache.getOrPut("url", "new script", cache.generation()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentLoadsReusePublishedSourceAndPreserveModuleState(boolean differentSource) throws Exception {
        try (Context first = engine(); Context second = engine()) {
            if (differentSource) {
                second.eval("js", "script = 'executions++; module.exports = {value: 2}';");
            }
            String startLoad = "var finishFetch; var loaded;"
                    + "fetch = () => new Promise(resolve => { finishFetch = () => resolve({ok: true, text: () => script}); });"
                    + "require('https://example.com/lib.js').then(value => { loaded = value; });";
            first.eval("js", startLoad);
            second.eval("js", startLoad);
            first.eval("js", "finishFetch();");
            String version = (String) cache.get("https://example.com/lib.js").getMember("version");
            first.eval("js", "loaded.value = 7;");
            // 后完成的请求必须使用已发布的源码，即使它拿到的响应内容不同。
            clock.set(Duration.ofMinutes(4).toNanos());
            second.eval("js", "finishFetch();");
            assertEquals(version, cache.get("https://example.com/lib.js").getMember("version"));
            assertEquals(7, require(first, ""));
            assertEquals(1, require(second, ""));
            assertEquals(1, first.eval("js", "executions").asInt());
            assertEquals(1, second.eval("js", "executions").asInt());
            // 并发完成也不能重置过期计时。
            clock.set(Duration.ofMinutes(5).toNanos());
            assertNull(cache.get("https://example.com/lib.js"));
        }
    }

    @Test
    void concurrentLoadsInOneEngineReuseTheSameExports() throws Exception {
        try (Context context = engine()) {
            context.eval("js", "var pending = []; var firstModule; var secondModule;"
                    + "fetch = () => new Promise(resolve => pending.push(() => resolve({ok: true, text: () => script})));"
                    + "require('https://example.com/lib.js').then(v => { firstModule = v; });"
                    + "require('https://example.com/lib.js').then(v => { secondModule = v; });");
            context.eval("js", "pending[0]();");
            context.eval("js", "firstModule.value = 7; pending[1]();");
            assertTrue(context.eval("js", "firstModule === secondModule").asBoolean());
            assertEquals(7, require(context, ""));
            assertEquals(1, context.eval("js", "executions").asInt());
        }
    }

    @Test
    void invalidatingFailedSourceDoesNotRemoveANewerVersion() {
        ProxyObject old = cache.getOrPut("url", "old", cache.generation());
        cache.clear();
        ProxyObject current = cache.getOrPut("url", "new", cache.generation());
        cache.invalidate("url", (String) old.getMember("version"));
        assertSame(current, cache.get("url"));
    }
}
