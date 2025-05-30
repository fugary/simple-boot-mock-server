package com.fugary.simple.mock.script;

import cn.hutool.crypto.asymmetric.AsymmetricCrypto;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.symmetric.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.hutool.crypto.asymmetric.KeyType.PrivateKey;
import static cn.hutool.crypto.asymmetric.KeyType.PublicKey;

/**
 * Create date 2025/5/29<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HuToolCryptoUtils {

    private static final Pattern PEM_PATTERN = Pattern.compile(
            "-----BEGIN (.*?)-----\\s*(.*?)\\s*-----END (.*?)-----",
            Pattern.DOTALL
    );

    /**
     * 检查pem字符串，提取类型和内容
     *
     * @param keyStr
     * @return
     */
    public static Pair<KeyType, String> autoCalcPemKey(String keyStr) {
        keyStr = StringUtils.trimToEmpty(keyStr);
        Matcher matcher = PEM_PATTERN.matcher(keyStr);
        if (matcher.find()) {
            String type = matcher.group(1).toUpperCase();
            String base64 = matcher.group(2).replaceAll("\\s+", "");
            if (type.contains("PRIVATE KEY")) {
                return Pair.of(PrivateKey, base64);
            } else if (type.contains("PUBLIC KEY")) {
                return Pair.of(PublicKey, base64);
            }
        }
        // 没有头尾，判断不了
        return Pair.of(null, keyStr.replaceAll("\\s+", ""));
    }

    /**
     * 对称加解密工具
     *
     * @param input    输入
     * @param password 密码，必须是 16、24 或 32 字节
     * @param config   配置信息
     * @param encrypt  是否加密
     * @return
     */
    public static String symmetricCrypto(String algorithm, String input, String password, Map<String, String> config, boolean encrypt) {
        if (StringUtils.isBlank(input) || StringUtils.isBlank(password)) {
            return input;
        }
        config = config == null ? Map.of() : config;
        String mode = StringUtils.defaultIfBlank(config.get("mode"), "ECB");
        String padding = StringUtils.defaultIfBlank(config.get("padding"), "PKCS5Padding");
        String output = StringUtils.defaultIfBlank(config.get("output"), "base64");
        byte[] iv = StringUtils.defaultIfBlank(config.get("iv"), "").getBytes(StandardCharsets.UTF_8);
        boolean base64 = StringUtils.equalsIgnoreCase(output, "base64");
        byte[] keyBytes = password.getBytes(StandardCharsets.UTF_8);
        // 校验 AES 密钥长度
        if (SymmetricAlgorithm.AES.getValue().equals(algorithm) && (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32)) {
            throw new IllegalArgumentException("AES 密钥长度必须是 16、24 或 32 字节");
        }
        SymmetricCrypto crypto;
        switch (algorithm) {
            case "AES":
                crypto = new AES(mode, padding, keyBytes, iv);
                break;
            case "DES":
                crypto = new DES(mode, padding, keyBytes, iv);
                break;
            case "DESede":
                crypto = new DESede(mode, padding, keyBytes, iv);
                break;
            case "SM4":
                crypto = new SM4(mode, padding, keyBytes, iv);
                break;
            default:
                throw new IllegalArgumentException("不支持的对称加密算法: " + algorithm);
        }
        return encrypt ? (base64 ? crypto.encryptBase64(input) : crypto.encryptHex(input)) : crypto.decryptStr(input);
    }

    /**
     * RSA加解密
     *
     * @param input
     * @param key
     * @param config
     * @param encrypt
     * @return
     */
    public static String asymmetricRSA(String input, String key, Map<String, String> config, boolean encrypt) {
        if (StringUtils.isBlank(input) || StringUtils.isBlank(key)) {
            return input;
        }
        config = config == null ? new HashMap<>() : config;
        String algorithm = StringUtils.defaultIfBlank(config.get("algorithm"), "RSA");
        String keyType = StringUtils.defaultIfBlank(config.get("keyType"), "PublicKey");
        String output = StringUtils.defaultIfBlank(config.get("output"), "base64");
        boolean base64 = StringUtils.equalsIgnoreCase(output, "base64");
        Pair<KeyType, String> keyPair = autoCalcPemKey(key);
        KeyType keyTypeEnum = Objects.requireNonNullElseGet(keyPair.getKey(), () -> keyType.equals("PublicKey") ? PublicKey : PrivateKey);
        String publicKey = null;
        String privateKey = null;
        switch (keyTypeEnum) {
            case PrivateKey:
                privateKey = keyPair.getValue();
                break;
            case PublicKey:
                publicKey = keyPair.getValue();
                break;
            default:
                throw new IllegalArgumentException("不支持的密钥类型: " + keyType);
        }
        AsymmetricCrypto crypto = new AsymmetricCrypto(algorithm, privateKey, publicKey);
        return encrypt ? (base64 ? crypto.encryptBase64(input, keyTypeEnum) : crypto.encryptHex(input, keyTypeEnum))
                : crypto.decryptStr(input, keyTypeEnum);
    }
}
