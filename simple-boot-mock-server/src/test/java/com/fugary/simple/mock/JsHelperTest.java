package com.fugary.simple.mock;

import com.fugary.simple.mock.script.JsHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * Create date 2025/5/30<br>
 *
 * @author gary.fu
 */
@Slf4j
public class JsHelperTest {

    @Test
    public void test() {
        JsHelper jsHelper = new JsHelper();
        log.info("getInitStr: \n{}", jsHelper.getInitStr());
        String ins = "5452493A462F542F3939392D303030303030303030312F53332F30384A554C2F503230";
        String o1 = jsHelper.decodeHex(ins);
        log.info("decoded: {}", o1);
        String o2 = jsHelper.encodeHex(o1);
        log.info("encoded: {}", o2);
        String input = "123456";
        String password = "1234567890123456";
        String result = jsHelper.encryptAES(input, password, null);
        log.info("encrypt: {}", result);
        String test = "NTM0XGrY6wanoipONqA4RgZsV/Dyv9wK3F+cMAs9ddMpA0ZpHLmG7MertuS2h92D";
        String result1 = jsHelper.decryptAES(test, password, Map.of("padding", "PKCS7Padding", "mode", "ECB"));
        log.info("decrypt: {}", result1);
//        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiX7hyRE2K6HJoskSIHgwSmDnGP1ennJeTOFIYJWMNmDN6PPn7MHDSDFV3JFNVqcuMKRRiKkf76aMDljiFCWEHX/slX3LQ2IopTkFicV66i7Ut+bvPQAfC7+ZNtLzLZhHFMF5ui1e6NE5AFX+YVwKmtlWS0XXIfL4ECFy7nunDYhgww0ig7CtSwcvDS8LQMCMqaURhJcigigyHSPp/dkmNWk5Y1L/YcjA0tVJ5tujDHuyvlw3dDJ2ao7/hRa+XXMo6cwv+wGnZyA6TPYPGUsLFn+L6xjBAWJ3haO5xV92wlQLoBdqngS//ojwznZnWsxOEzdK+VDPvXSqBNvip0eBVwIDAQAB";
//        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCJfuHJETYrocmiyRIgeDBKYOcY/V6ecl5M4UhglYw2YM3o8+fswcNIMVXckU1Wpy4wpFGIqR/vpowOWOIUJYQdf+yVfctDYiilOQWJxXrqLtS35u89AB8Lv5k20vMtmEcUwXm6LV7o0TkAVf5hXAqa2VZLRdch8vgQIXLue6cNiGDDDSKDsK1LBy8NLwtAwIyppRGElyKCKDIdI+n92SY1aTljUv9hyMDS1Unm26MMe7K+XDd0MnZqjv+FFr5dcyjpzC/7AadnIDpM9g8ZSwsWf4vrGMEBYneFo7nFX3bCVAugF2qeBL/+iPDOdmdazE4TN0r5UM+9dKoE2+KnR4FXAgMBAAECggEACyv5EopIeYEazhRCWcsKOXDoNhqYLo2iZeLEK7za+Kwi7vOqe40+3cXMcTCJNSo+UYt4aH89bg59QMkuDW8C7hav1XG0K1R1cv9QGOKn2xFVDWBBPhadbynxPAgl5CWEWQZjaqI3HPmrBQD3u1dsMSrJnTIeic6hI0ZkfUYky6+TlyLCOD2UWCjXDfrMKdHdeYN57wXu9cjmPYV6rR8bSLALgAZfNwB3AO9v6eOFpq5C3QXEREErHSODaoIJhtK4KaS5dgDaZLjMOnlh8nDMu6zCawaRrsGaodDpVzVcspeP4FRhLmzPQr4te0wzjMxSLKxWQzUL5qh6ZaPa0uPZGQKBgQC+oEQttdmcVRLy510Ld9Vvb4x8jq4Xx8vWuGFSu9//zoll/5DzUkOb0xnMfxsy7AY7k9A6A442ZYv6ssxoJzaITViOoCcGAlIQb/K9G/myXZDRpi/0oO/kE67SDABWORssZg5GUAeowM8mys1d/oQ/V6uGxYKhK6I+8A1d7HhkYwKBgQC4phwDUqIbgHMYSthi4tRcSIVat7UfIBgvbsXyVMfzHZePT9xtx4TbFphOX0UA0rnBC0SJVmt0paYQHjoujn2sd983ppr1vOr2gYty4aN/o/K2oDRi7rAkOinUDj0L3a16zZDhokPGvJ97MkKk1d6hUFyn6HTzBMyliPGv6deffQKBgA03j4Kb2Tq9q1KsOAdTh7jyQwtf2TJM6BU7M4RJAn76Ewy/a41zZrynJmLHbVtaghQfa5CaLoIXCF2ZiBKwpVdOBZXeBNal0orROENkbSuw0Rm4LBsKDZgCxVxK+LsBuqCZcuPtKJrKw5Is/ZBG1WSO75WonCYQoG88tbCNdMuBAoGAH7u0bsHByFYv8YnTXFM9ScrQ2EFgKEQ8w9IWxMnbPyXjySZhbKBwbbKZZEatNjIA6HNGSRAIEZjyTCzVtPCaGozTWN4SHY9lFg43Z/vtPF9xs/8eYa5BqUgxrhbwWNy1FrjDYHOhkEMUaXi+yVNUGocUFWs2xhkaDozVdHUMsIUCgYB9oYdF1iFM+80v/TbpHOj1m60czoSmC1RKNuGXFReXoGt0OL7CEP1sji8s5X0o5q+VpOK3aDc9KGVX+kSdbdom69n9QireVI1pWiEs6UWnQRM7KsQ5UcFZHYfihSZGt3pzzG7c77zCZ8DtUD5pQAvaEA1+LD9BgoxiBh5rJwTjbg==";
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArO1cneOdxuSbLY3EsShu\n" +
                "kCiy7b1/AhalhCIUToV8gFdHINOZ1t3HDDB19xB8+6xxyRp5ler8BSfpA1f6s0VF\n" +
                "jYj+2ApN+4Bs8EanAWzpZJP11Iepmf6NprIY37+E9vS7gIvwLJzCi8BhPmFSx4wu\n" +
                "2NaN1BZzkNsmzJuq/gG0KsWcOcxygtWFU5m+mypPwI2AHTrcVYxluo+GeZmH+ubl\n" +
                "K2wrL+mCT8JJBawSo+ZwC8Wns3F5Poy4f3NbroQEGcUylQNKw9rZ3t6meFEboy0B\n" +
                "0r5JCy3ygLMkxgqOgV86hoII59PIn8bSN9umq/0m2rrM+SMPKnF/TCAuamy/QSMv\n" +
                "jwIDAQAB\n" +
                "-----END PUBLIC KEY-----";
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEpAIBAAKCAQEArO1cneOdxuSbLY3EsShukCiy7b1/AhalhCIUToV8gFdHINOZ\n" +
                "1t3HDDB19xB8+6xxyRp5ler8BSfpA1f6s0VFjYj+2ApN+4Bs8EanAWzpZJP11Iep\n" +
                "mf6NprIY37+E9vS7gIvwLJzCi8BhPmFSx4wu2NaN1BZzkNsmzJuq/gG0KsWcOcxy\n" +
                "gtWFU5m+mypPwI2AHTrcVYxluo+GeZmH+ublK2wrL+mCT8JJBawSo+ZwC8Wns3F5\n" +
                "Poy4f3NbroQEGcUylQNKw9rZ3t6meFEboy0B0r5JCy3ygLMkxgqOgV86hoII59PI\n" +
                "n8bSN9umq/0m2rrM+SMPKnF/TCAuamy/QSMvjwIDAQABAoIBAC6Axl9PTqal43Uk\n" +
                "orhhfxoVV8jHJNiS7n/SClPzYb/BuMcDIGO/0D9e6No//NxRS2ghToEaaBua1/am\n" +
                "OizrfADLznyaiM4Rgzo6Rb8EYt9BSuKUeO+InN8vFbFV95PRzHqNJ1FX9plOatgz\n" +
                "o+c1s4Nsb0eRohJaXNy5IAqeuqyldTaGcOY8PcpEZY0SpOdhKOKbguFfDntm/2td\n" +
                "ri1elS/gfivckyw9VNM2Lcow6yAyGd1RfF6yCWl0kY/pdrwbOA5epdewYHFcMGEC\n" +
                "3HZorEk1zA2/at1BrfRMWO1/lGIdP6OgAl9npzcl0OZrI5Z7wxSi39SPre8HVnBC\n" +
                "giflTUECgYEA83qwL7OCqLMxjKbQGj/6kiak5znQkETsD4n/nJReynH0Sxf01RvB\n" +
                "ogY5ex00djco7HAwfoU+8AH76G5f4UWYWghLOqXqxQlixmoPr3HcGph9pxk9t2dH\n" +
                "Uey9iDCkIyPCJFNqaAuYQXPoCGRMgptYflGBn2zNwcXjz1emcGDgHo0CgYEAtdHh\n" +
                "7zC+wgmKenH3o2cnNBivrN++ZHjty3+83/fVzvsoa1w+mNr7aFr5QlemoHPahdEX\n" +
                "eDkJVjWuXxT8Ll5Qk53vqf8rWBdr0EreGpkg/NB5uqUxS/9TQfhpEgZ35b3Uqwuk\n" +
                "q8Wn51sPvT7Mu+4MweyvqjM/EgY3pM1i6ueXPYsCgYBqwiD+yXdBAVF4mqwk1mjI\n" +
                "LKmhuXf5yj63kYrC3Uuy+MRlKNvxlXoR0SjjEqsuKoGHKqoYz9QI6NXL1Jm17Q4g\n" +
                "bAKQdK+8NaFZ3qeCBiq8fqCZ5Ddrc52SDnwMe0d9IPC/fNPrSyo3iNr00wW5PuRO\n" +
                "fu6XON3/WQXKpGQWYmPjAQKBgQCXMbsxzlUywgBXpiIJzaO6HLUpir2mizEjQkvr\n" +
                "6wnvHsgTzxd2wNLDirMwSfcUAoM7OA7OOI/dErK3+plibaePuYwszzZaAM/02+0q\n" +
                "LSBTW4F+hhQiYGX5u/qTtLFLaLpKqln7RVhEeWLzOh/CP0h4krvZStgtwuHOt8SE\n" +
                "FqWb1QKBgQDsTz9q5FPkz4yZEsDKpagE0UBYzqw0dnqMgh5UG8Hg0wgRJg78+W6A\n" +
                "mVaSu9sYWK/A0bVfvi0IOcvpB6y5yGL+bbzKYTOhAvWCv9irp62KopwN3Y6E0ojM\n" +
                "112DCfUhybatxQkCggLlNI4MI+dw76IlC3FKHtpqVaKCoIescjOJ4w==\n" +
                "-----END RSA PRIVATE KEY-----";
        String result2 = jsHelper.encryptRSA("{\n" +
                "  \"name\": \"abc\",\n" +
                "  \"age\": 99\n" +
                "}", publicKey, null);
        log.info("encryptRSA: {}", result2);
        String result3 = jsHelper.decryptRSA(result2, privateKey, null);
        log.info("decryptRSA-1: {}", result3);
        result2 = "dvGRgB/RCELK392rZ0G/LJr8GUYcFkNPSZQqAJOV6Q7JvtCWjJ0jQ2DQ96CX+UCbiQcuofb5h/jfZrGvGjKKh1B/tgmWGuX2lXESQghSKa/0cAHe2QVNorghY/FC4KgdUDXl+kY1FbhLpj+WGWk80n+5HH1XXjrWVIYh0UWG1OAdE0H4JS57of+9tOShGo9G7E0KSvews5dqAi7TcoQ/DqPreJbw7D2VISNLuoQbaV5CxcZhIjY1IjIu8qb0VjtiU05ZTWUc23lGvcwS3YfEnZ9rKavc22LwZ/IFutG7trBaRbjnn9hiMcoSV+VJEx8lv40wMLPF0aS7S1WjgHnxow==";
        result2 = "dvGRgB/RCELK392rZ0G/LJr8GUYcFkNPSZQqAJOV6Q7JvtCWjJ0jQ2DQ96CX+UCbiQcuofb5h/jfZrGvGjKKh1B/tgmWGuX2lXESQghSKa/0cAHe2QVNorghY/FC4KgdUDXl+kY1FbhLpj+WGWk80n+5HH1XXjrWVIYh0UWG1OAdE0H4JS57of+9tOShGo9G7E0KSvews5dqAi7TcoQ/DqPreJbw7D2VISNLuoQbaV5CxcZhIjY1IjIu8qb0VjtiU05ZTWUc23lGvcwS3YfEnZ9rKavc22LwZ/IFutG7trBaRbjnn9hiMcoSV+VJEx8lv40wMLPF0aS7S1WjgHnxow==";
        String result4 = jsHelper.decryptRSA(result2, privateKey, Map.of("algorithm", "RSA/ECB/PKCS1Padding"));
        log.info("decryptRSA-2: {}", result4);
    }
}
