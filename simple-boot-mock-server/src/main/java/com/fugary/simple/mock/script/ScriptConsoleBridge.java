package com.fugary.simple.mock.script;

import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Captures script console output for frontend preview diagnose only.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScriptConsoleBridge {

    private static final String BRIDGE_KEY = "__simpleMockScriptConsoleBridge__";

    private static final String CONSOLE_SCRIPT = String.join("\n",
            "(() => {",
            "  const bridge = " + BRIDGE_KEY + ";",
            "  const stringify = value => {",
            "    if (typeof value === 'string') return value;",
            "    try { return mockStringify(value); } catch (e) {",
            "      try { return String(value); } catch (ignore) { return ''; }",
            "    }",
            "  };",
            "  const emit = level => (...args) => bridge(level, args.map(stringify).join(' '));",
            "  return {",
            "    log: emit('log'),",
            "    info: emit('info'),",
            "    warn: emit('warn'),",
            "    error: emit('error'),",
            "    debug: emit('debug'),",
            "    clear() {}",
            "  };",
            "})()");

    public static void install(ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException {
        scriptContext.setAttribute(BRIDGE_KEY, (ProxyExecutable) args -> {
            String level = args.length > 0 && args[0].isString() ? args[0].asString() : "log";
            String message = args.length > 1 && args[1].isString() ? args[1].asString() : "";
            MockDiagnoseRecorder.of(MockDiagnoseContext.get()).scriptConsole(level, message);
            writeBackendLog(level, message);
            return null;
        }, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("console", scriptEngine.eval(CONSOLE_SCRIPT, scriptContext),
                ScriptContext.ENGINE_SCOPE);
    }

    private static void writeBackendLog(String level, String message) {
        switch (level) {
            case "error":
                log.error("[script console.error] {}", message);
                break;
            case "warn":
                log.warn("[script console.warn] {}", message);
                break;
            case "debug":
                log.debug("[script console.debug] {}", message);
                break;
            case "info":
                log.info("[script console.info] {}", message);
                break;
            case "log":
            default:
                log.info("[script console.log] {}", message);
                break;
        }
    }
}
