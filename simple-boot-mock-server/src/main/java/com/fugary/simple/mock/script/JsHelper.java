package com.fugary.simple.mock.script;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.XmlUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    public String btoa(Object input) {
        if (input instanceof String) {
            return Base64.getEncoder().encodeToString(((String) input).getBytes());
        } else if (input instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) input);
        } else if (input instanceof List) {
            byte[] bytes = SimpleMockUtils.listToByteArray((List) input);
            return Base64.getEncoder().encodeToString(bytes);
        }
        return input != null ? input.toString() : null;
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
     * AES加密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String encryptAES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.AES.getValue(), input, password, config, true);
    }

    /**
     * AES解密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String decryptAES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.AES.getValue(), input, password, config, false);
    }

    /**
     * DES加密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String encryptDES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.DES.getValue(), input, password, config, true);
    }

    /**
     * DES解密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String decryptDES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.DES.getValue(), input, password, config, false);
    }

    /**
     * 3DES加密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String encrypt3DES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.DESede.getValue(), input, password, config, true);
    }

    /**
     * 3DES解密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String decrypt3DES(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto(SymmetricAlgorithm.DESede.getValue(), input, password, config, false);
    }

    /**
     * SM4加密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String encryptSM4(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto("SM4", input, password, config, true);
    }

    /**
     * SM4解密
     *
     * @param input
     * @param password
     * @param config
     * @return
     */
    public String decryptSM4(String input, String password, Map<String, String> config) {
        return HuToolCryptoUtils.symmetricCrypto("SM4", input, password, config, false);
    }

    /**
     * RSA加密
     *
     * @param input
     * @param key
     * @param config
     * @return
     */
    public String encryptRSA(String input, String key, Map<String, String> config) {
        return HuToolCryptoUtils.asymmetricRSA(input, key, config, true);
    }

    /**
     * RSA解密
     *
     * @param input
     * @param key
     * @param config
     * @return
     */
    public String decryptRSA(String input, String key, Map<String, String> config) {
        return HuToolCryptoUtils.asymmetricRSA(input, key, config, false);
    }

    /**
     * 将XML字符串转换成JSON格式
     *
     * @param xml
     * @return
     */
    public String xml2Json(String xml) {
        SimpleResult<String> result = XmlUtils.xml2Json(xml);
        if (result.isSuccess()) {
            return result.getResultData();
        }
        return JsonUtils.toJson(result);
    }

    /**
     * 将JSON字符串转换成XML格式
     *
     * @param json
     * @return
     */
    public String json2Xml(String json, String rootName) {
        SimpleResult<String> result = JsonUtils.json2Xml(json, rootName);
        if (result.isSuccess()) {
            return result.getResultData();
        }
        return XmlUtils.toXml(result, rootName);
    }

    /**
     * java对象转json数据
     *
     * @param java
     * @return
     */
    public String java2Json(Object java) {
        return JsonUtils.toJson(java);
    }

    /**
     * 初始化方法
     *
     * @return
     */
    public String getInitStr() {
        StringBuilder sb = new StringBuilder();
        Set<String> excludeMethods = Set.of("getInitStr", "main");
        Set<String> hasOptionalMethods = Set.of("json2Xml");
        for (Method method : getClass().getDeclaredMethods()) {
            if (!excludeMethods.contains(method.getName())) {
                sb.append("if(!globalThis.").append(method.getName()).append("){\n")
                        .append("\tglobalThis.").append(method.getName())
                        .append(" = ");
                if (StringUtils.startsWithAny(method.getName(), "encrypt", "decrypt")) {
                    sb.append("(input, password, config) => {\n")
                            .append("\t\tinput = (typeof input === 'string') ? input : JSON.stringify(input);\n")
                            .append("\t\tpassword = (typeof password === 'string') ? password : JSON.stringify(password);\n")
                            .append("\t\treturn JsHelper.").append(method.getName())
                            .append("(input, password, config || {});\n")
                            .append("\t}\n");
                } else if(hasOptionalMethods.contains(method.getName())) {
                    String paramsStr = Arrays.stream(method.getParameters()).map(Parameter::getName)
                            .collect(Collectors.joining(","));
                    sb.append("(").append(paramsStr).append(") => {\n")
                            .append("\t\treturn JsHelper.").append(method.getName())
                            .append("(").append(paramsStr).append(");\n")
                            .append("\t}\n");
                } else {
                    sb.append("JsHelper.").append(method.getName()).append(";\n");
                }
                sb.append("}\n");
            }
        }
        return sb.toString();
    }
}
