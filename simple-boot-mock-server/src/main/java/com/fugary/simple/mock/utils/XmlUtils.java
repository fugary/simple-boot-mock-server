package com.fugary.simple.mock.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Create date 2024/7/24<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XmlUtils {

    /**
     * 默认Mapper
     */
    private static final ObjectMapper MAPPER = new XmlMapper();

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * 对象转xml
     *
     * @param input
     * @return
     */
    public static String toXml(Object input) {
        String result = StringUtils.EMPTY;
        try {
            result = getMapper().writeValueAsString(input);
        } catch (Exception e) {
            log.error("输出xml错误", e);
        }
        return result;
    }

    /**
     * xml转对象
     *
     * @param input
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T fromXml(String input, Class<T> clazz) {
        T result = null;
        try {
            result = getMapper().readValue(input, clazz);
        } catch (Exception e) {
            log.error("输出xml错误", e);
        }
        return result;
    }

}
