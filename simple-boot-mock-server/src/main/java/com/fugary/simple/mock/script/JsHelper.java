package com.fugary.simple.mock.script;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 工具函数
 */
public class JsHelper {
    /**
     * Base64编码
     *
     * @param input
     * @return
     */
    public String btoa(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    /**
     * Base64解码
     *
     * @param input
     * @return
     */
    public String atob(String input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * 初始化方法
     *
     * @return
     */
    public String getInitStr() {
        StringBuilder sb = new StringBuilder();
        for (Method method : getClass().getDeclaredMethods()) {
            if (!"getInitStr".equals(method.getName())) {
                sb.append("if(!globalThis.").append(method.getName()).append("){\n")
                        .append("\tglobalThis.").append(method.getName()).append(" = JsHelper.").append(method.getName())
                        .append(";\n")
                        .append("}\n");
            }
        }
        return sb.toString();
    }

}
