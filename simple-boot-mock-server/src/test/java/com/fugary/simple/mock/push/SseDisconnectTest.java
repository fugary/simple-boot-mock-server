package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.utils.MockEventStreamUtils;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SseDisconnectTest {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final CountDownLatch firstEvent = new CountDownLatch(1);
    private final CountDownLatch scopeClosed = new CountDownLatch(1);
    private final MockPostScriptProcessor processor = new DefaultMockPostScriptProcessorImpl() {
        @Override
        public AutoCloseable openSseScriptScope(MockRequest request, MockData data) {
            return scopeClosed::countDown;
        }

        @Override
        public Object processSse(MockRequest request, MockData data, Object item) {
            firstEvent.countDown();
            return item;
        }
    };

    @AfterEach
    void stopWorker() {
        worker.shutdownNow();
    }

    @Test
    void localDelayShouldEndOnDisconnect() throws Exception {
        MockData data = new MockData();
        data.setResponseBody("{\"stream\":[{\"data\":\"later\",\"delay\":60000}]}");
        SseEmitter emitter = MockEventStreamUtils.processSseRequest(new MockHttpServletRequest(),
                new MockHttpServletResponse(), data, worker, processor, new MockRequest());
        disconnectWhenReady(emitter, firstEvent);
    }

    @Test
    void idleUpstreamReadShouldEndOnDisconnect() throws Exception {
        CountDownLatch releaseServer = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/events", exchange -> {
            try {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("data: first\n\n".getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                releaseServer.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        CountDownLatch nextRead = new CountDownLatch(1);
        // 等工作线程进入下一次读取再断开，避免只测到两次 readLine 之间检查取消标记的情况。
        HttpResponseInterceptor observeRead = (response, context) ->
                response.setEntity(new HttpEntityWrapper(response.getEntity()) {
                    @Override
                    public InputStream getContent() throws IOException {
                        return new FilterInputStream(super.getContent()) {
                            @Override
                            public int read(byte[] bytes, int offset, int length) throws IOException {
                                if (firstEvent.getCount() == 0) {
                                    nextRead.countDown();
                                }
                                return super.read(bytes, offset, length);
                            }
                        };
                    }
                });
        try (CloseableHttpClient client = HttpClients.custom().addInterceptorLast(observeRead).build()) {
            DefaultMockSsePushProcessorImpl proxy = new DefaultMockSsePushProcessorImpl();
            ReflectionTestUtils.setField(proxy, "mockHttpClient", client);
            ReflectionTestUtils.setField(proxy, "defaultPushProcessor", new DefaultMockPushProcessorImpl());
            ReflectionTestUtils.setField(proxy, "eventStreamThreadPool", worker);
            ReflectionTestUtils.setField(proxy, "mockPostScriptProcessor", processor);
            MockParamsVo params = new MockParamsVo();
            params.setTargetUrl("http://127.0.0.1:" + server.getAddress().getPort());
            params.setRequestPath("/events");
            params.setMethod("GET");
            disconnectWhenReady(proxy.processSseProxy(params, new MockRequest(), null), nextRead);
        } finally {
            releaseServer.countDown();
            server.stop(0);
        }
    }

    private void disconnectWhenReady(SseEmitter emitter, CountDownLatch ready) throws InterruptedException {
        assertTrue(ready.await(5, TimeUnit.SECONDS), "工作线程应已进入待取消的操作");
        ((Runnable) ReflectionTestUtils.getField(emitter, "completionCallback")).run();
        assertTrue(scopeClosed.await(2, TimeUnit.SECONDS), "断开后应及时关闭脚本引擎作用域");
    }
}
