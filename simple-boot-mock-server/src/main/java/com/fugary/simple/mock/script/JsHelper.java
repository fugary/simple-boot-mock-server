package com.fugary.simple.mock.script;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

/**
 * 工具函数
 */
@Slf4j
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
        try {
            byte[] decoded = Base64.getDecoder().decode(input);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("执行Base64解码失败", e);
        }
        return input;
    }

    /**
     * 十六进制解码
     * @param input
     * @return
     */
    public String decodeHex(String input) {
        if (StringUtils.isNotBlank(input)) {
            try {
                return new String(Hex.decodeHex(input.toCharArray()), StandardCharsets.UTF_8);
            } catch (DecoderException e) {
                log.error("解码失败", e);
            }
        }
        return input;
    }

    /**
     * 十六进制编码
     * @param input
     * @return
     */
    public String encodeHex(String input) {
        if (StringUtils.isNotBlank(input)) {
            return new String(Hex.encodeHex(input.getBytes(StandardCharsets.UTF_8)));
        }
        return input;
    }

    /**
     * MD5加密
     * @param input
     * @return
     */
    public String md5Hex(String input) {
        if (StringUtils.isNotBlank(input)) {
            return DigestUtils.md5Hex(input);
        }
        return input;
    }

    /**
     * MD5加密
     * @param input
     * @return
     */
    public String md5Base64(String input) {
        if (StringUtils.isNotBlank(input)) {
            byte[] bytes = DigestUtils.md5(input);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return input;
    }

    /**
     * sha1加密
     * @param input
     * @return
     */
    public String sha1Hex(String input) {
        if (StringUtils.isNotBlank(input)) {
            return DigestUtils.sha1Hex(input);
        }
        return input;
    }

    /**
     * sha1加密
     * @param input
     * @return
     */
    public String sha1Base64(String input) {
        if (StringUtils.isNotBlank(input)) {
            byte[] bytes = DigestUtils.sha1(input);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return input;
    }

    /**
     * sha256加密
     * @param input
     * @return
     */
    public String sha256Hex(String input) {
        if (StringUtils.isNotBlank(input)) {
            return DigestUtils.sha256Hex(input);
        }
        return input;
    }

    /**
     * sha256加密
     * @param input
     * @return
     */
    public String sha256Base64(String input) {
        if (StringUtils.isNotBlank(input)) {
            byte[] bytes = DigestUtils.sha256(input);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return input;
    }

    /**
     * 初始化方法
     *
     * @return
     */
    public String getInitStr() {
        StringBuilder sb = new StringBuilder();
        Set<String> excludeMethods = Set.of("getInitStr", "main");
        for (Method method : getClass().getDeclaredMethods()) {
            if (!excludeMethods.contains(method.getName())) {
                sb.append("if(!globalThis.").append(method.getName()).append("){\n")
                        .append("\tglobalThis.").append(method.getName()).append(" = JsHelper.").append(method.getName())
                        .append(";\n")
                        .append("}\n");
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        JsHelper jsHelper = new JsHelper();
        log.info("getInitStr: \n{}", jsHelper.getInitStr());
        String ins = "5452493A462F542F3939392D303030303030303030312F53332F30384A554C2F503230";
        String o1 = jsHelper.decodeHex(ins);
        log.info("decoded: {}", o1);
        String o2 = jsHelper.encodeHex(o1);
        log.info("encoded: {}", o2);
    }

}
