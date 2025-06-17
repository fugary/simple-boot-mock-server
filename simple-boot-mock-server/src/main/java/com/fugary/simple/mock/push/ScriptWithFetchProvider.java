package com.fugary.simple.mock.push;

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
     * fetch功能
     *
     * @param script
     * @param scriptEngine
     * @param scriptContext
     * @return
     * @throws ScriptException
     */
    Object internalEval(String script, ScriptEngine scriptEngine, ScriptContext scriptContext) throws ScriptException;
}
