package com.fugary.simple.mock.script;

/**
 * Create date 2024/7/4<br>
 *
 * @author gary.fu
 */
public interface ScriptEngineProvider {
    /**
     * 数据模拟
     * @param template
     * @return
     */
    String mock(String template);

    /**
     * 数据执行
     * @param script
     * @return
     */
    Object eval(String script);

    /**
     * 数据执行成string输出
     *
     * @param script
     * @return
     */
    String evalStr(String script);
}
