package com.fugary.simple.mock.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fugary.simple.mock.entity.mock.MockSchema;
/**
 * Create date 2024/10/23<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class SchemaJsonUtils {

    /**
     * 默认Mapper
     */
    private static final ObjectMapper MAPPER = Json.mapper();

    /**
     * 默认Mapper
     */
    private static final ObjectMapper MAPPER_V31 = Json31.mapper();

    /**
     * 最简单mapper
     */
    public static final ObjectMapper FORMATED_MAPPER = new ObjectMapper();

    private static final Pattern REF_PATTERN = Pattern.compile("\"(?:#/components/schemas/|#/definitions/)([^\"]+)\"");

    static {
        FORMATED_MAPPER.enable(SerializationFeature.INDENT_OUTPUT); // 开启缩进
    }

    public static ObjectMapper getMapper(boolean v31) {
        return v31 ? MAPPER_V31 : MAPPER;
    }

    /**
     * 对象转yaml
     *
     * @param input
     * @return
     */
    public static String toJson(Object input, boolean v31) {
        String result = StringUtils.EMPTY;
        try {
            result = getMapper(v31).writeValueAsString(input);
        } catch (Exception e) {
            log.error("输出json错误", e);
        }
        return result;
    }

    /**
     * yaml转对象
     *
     * @param input
     * @param clazz
     * @param v31
     * @param <T>
     * @return
     */
    public static <T> T fromJson(String input, Class<T> clazz, boolean v31) {
        T result = null;
        try {
            result = getMapper(v31).readValue(input, clazz);
        } catch (Exception e) {
            log.error("输出json错误", e);
        }
        return result;
    }


    public static <T> T fromJson(String json, TypeReference<T> typeReference, boolean v31) {
        T result = null;
        try {
            if (StringUtils.isNotBlank(json)) {
                result = getMapper(v31).readValue(json, typeReference);
            }
        } catch (IOException e) {
            log.error("将Json转换成对象出错", e);
        }

        return result;
    }

    public static boolean isV31(OpenAPI openAPI) {
        return SpecVersion.V31.equals(openAPI.getSpecVersion());
    }

    public static boolean isV31(SpecVersion specVersion) {
        return SpecVersion.V31.equals(specVersion);
    }

    /**
     * 自定义Mapper
     * @param mapper
     * @param input
     * @return
     */
    public static String toJson(ObjectMapper mapper, Object input) {
        String result = StringUtils.EMPTY;
        try {
            result = mapper.writeValueAsString(input);
        } catch (Exception e) {
            log.error("输出json错误", e);
        }
        return result;
    }

    public static MockSchema filterComponentSchema(MockSchema componentSchema, List<MockSchema> schemas) {
        if (componentSchema == null || StringUtils.isBlank(componentSchema.getRequestBodySchema()) || schemas == null || schemas.isEmpty()) {
            return componentSchema;
        }
        try {
            Set<String> usedRefs = new HashSet<>();
            for (MockSchema schema : schemas) {
                extractRefs(schema.getParametersSchema(), usedRefs);
                extractRefs(schema.getRequestBodySchema(), usedRefs);
                extractRefs(schema.getResponseBodySchema(), usedRefs);
            }
            ObjectMapper mapper = getMapper(false);
            JsonNode openApiNode = mapper.readTree(componentSchema.getRequestBodySchema());
            JsonNode componentsNode = openApiNode.path("components");
            JsonNode schemasNode = componentsNode.path("schemas");
            if (schemasNode.isObject()) {
                ObjectNode newSchemasNode = mapper.createObjectNode();
                if (!usedRefs.isEmpty()) {
                    Set<String> resolvedRefs = new HashSet<>();
                    resolveRefs(schemasNode, usedRefs, resolvedRefs);
                    resolvedRefs.forEach(ref -> {
                        if (schemasNode.has(ref)) {
                            newSchemasNode.set(ref, schemasNode.get(ref));
                        }
                    });
                }
                ((ObjectNode) componentsNode).set("schemas", newSchemasNode);
                componentSchema.setRequestBodySchema(mapper.writeValueAsString(openApiNode));
            }
        } catch (Exception e) {
            log.error("过滤组件Schema失败", e);
        }
        return componentSchema;
    }

    private static void extractRefs(String schemaStr, Set<String> refs) {
        if (StringUtils.isNotBlank(schemaStr)) {
            Matcher matcher = REF_PATTERN.matcher(schemaStr);
            while (matcher.find()) {
                refs.add(matcher.group(1));
            }
        }
    }

    private static void resolveRefs(JsonNode schemasNode, Set<String> usedRefs, Set<String> resolvedRefs) {
        Queue<String> queue = new LinkedList<>(usedRefs);
        while (!queue.isEmpty()) {
            String ref = queue.poll();
            if (resolvedRefs.add(ref)) {
                JsonNode componentNode = schemasNode.get(ref);
                if (componentNode != null) {
                    Matcher matcher = REF_PATTERN.matcher(componentNode.toString());
                    while (matcher.find()) {
                        String childRef = matcher.group(1);
                        if (!resolvedRefs.contains(childRef)) {
                            queue.add(childRef);
                        }
                    }
                }
            }
        }
    }
}
