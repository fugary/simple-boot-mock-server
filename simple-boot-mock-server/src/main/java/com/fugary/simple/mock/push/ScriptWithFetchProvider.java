package com.fugary.simple.mock.push;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Create date 2025/6/17<br>
 *
 * @author gary.fu
 */
public interface ScriptWithFetchProvider {

    /**
     * 支持Promise的执行功能
     *
     * @param script
     * @param scriptEngine
     * @param scriptContext
     * @return
     * @throws ScriptException
     */
    Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException;

    /**
     * fetch函数
     * @param context
     * @return
     */
    ProxyExecutable getFetchFunction(Context context);
}
