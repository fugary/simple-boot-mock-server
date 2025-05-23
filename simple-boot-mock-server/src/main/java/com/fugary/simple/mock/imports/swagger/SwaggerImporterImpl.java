package com.fugary.simple.mock.imports.swagger;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.SchemaJsonUtils;
import com.fugary.simple.mock.web.vo.export.*;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class SwaggerImporterImpl implements MockGroupImporter {

    public static final Pattern SCHEMA_COMPONENT_PATTERN = Pattern.compile("\"" + Components.COMPONENTS_SCHEMAS_REF + "([^\"]+)\"");

    @Override
    public boolean isSupport(String type) {
        return "swagger".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(data, null, parseOptions);
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI != null) {
            Map<String, List<Triple<String, PathItem, List<Pair<String, Operation>>>>> pathMap = openAPI.getPaths().entrySet().stream().map(entry -> {
                PathItem pathItem = entry.getValue();
                List<Pair<String, Operation>> operations = getAllOperationsInAPath(pathItem);
                return Triple.of(entry.getKey(), entry.getValue(), operations);
            }).collect(Collectors.groupingBy(triple -> {
                List<Pair<String, Operation>> operations = triple.getRight();
                Pair<String, Operation> firstOptionPair = operations.get(0);
                return getTagName0(firstOptionPair.getRight().getTags());
            }, LinkedHashMap::new, Collectors.toList()));
            List<Tag> tags = openAPI.getTags();
            if (CollectionUtils.isEmpty(tags)) {
                tags = pathMap.keySet().stream().map(key -> new Tag().name(key)).collect(Collectors.toList());
            }
            List<ExportGroupVo> mockGroups = tags.stream().filter(tag -> pathMap.get(tag.getName()) != null)
                    .map(tag -> toMockGroup(tag, openAPI, pathMap.get(tag.getName()))).collect(Collectors.toList());
            ExportMockVo exportMockVo = new ExportMockVo();
            exportMockVo.setGroups(mockGroups);
            return exportMockVo;
        }
        return null;
    }

    protected String getTagName0(List<String> tags) {
        return CollectionUtils.isNotEmpty(tags) ? tags.get(0) : "default";
    }

    protected List<ExportSchemaVo> calcComponentSchemas(OpenAPI openAPI, ExportGroupVo groupVo) {
        List<ExportSchemaVo> schemas = new ArrayList<>();
        if (openAPI.getComponents() != null) {
            Map<String, Schema> groupComponentSchemas = new HashMap<>();
            calcGroupComponents(openAPI, groupVo, groupComponentSchemas);
            if (!groupComponentSchemas.isEmpty()) {
                OpenAPI groupOpenAPI = new OpenAPI()
                        .specVersion(openAPI.getSpecVersion())
                        .components(new Components().schemas(groupComponentSchemas));
                ExportSchemaVo groupSchemaVo = new ExportSchemaVo();
                groupSchemaVo.setBodyType(MockConstants.MOCK_SCHEMA_BODY_TYPE_COMPONENT);
                groupSchemaVo.setRequestBodySchema(SchemaJsonUtils.toJson(groupOpenAPI, isV31(openAPI)));
                schemas.add(groupSchemaVo);
            }
        }
        return schemas;
    }

    protected void calcGroupComponents(OpenAPI openAPI, ExportGroupVo groupVo, Map<String, Schema> groupComponentSchemas) {
        Map<String, Schema> componentSchemas = openAPI.getComponents().getSchemas();
        if (componentSchemas != null) {
            groupVo.getRequests().stream().flatMap(request -> request.getDataList().stream()
                    .flatMap(data -> data.getSchemas().stream())).flatMap(schema -> {
                Set<String> schemaSet = calcComponentKeys(schema.getParametersSchema());
                schemaSet.addAll(calcComponentKeys(schema.getRequestBodySchema()));
                schemaSet.addAll(calcComponentKeys(schema.getResponseBodySchema()));
                return schemaSet.stream();
            }).distinct().forEach(schemaKey -> {
                if (componentSchemas.get(schemaKey) != null) {
                    groupComponentSchemas.put(schemaKey, componentSchemas.get(schemaKey));
                }
            });
        }
    }

    protected Set<String> calcComponentKeys(String content){
        Set<String> results = new HashSet<>();
        if (StringUtils.isNotBlank(content)) {
            Matcher matcher = SCHEMA_COMPONENT_PATTERN.matcher(content);
            while (matcher.find()) {
                results.add(matcher.group(1));
            }
        }
        return results;
    }

    /**
     * 解析成MockGroup
     *
     * @param tag
     * @param openAPI
     * @param valueList
     * @return
     */
    protected ExportGroupVo toMockGroup(Tag tag, OpenAPI openAPI, List<Triple<String, PathItem, List<Pair<String, Operation>>>> valueList) {
        Info info = openAPI.getInfo();
        ExportGroupVo group = new ExportGroupVo();
        group.setGroupName(info.getTitle() + "-" + tag.getName());
        group.setDescription(tag.getDescription());
        group.setStatus(1);
        group.setGroupPath(DigestUtils.md5Hex(SchemaJsonUtils.toJson(group, isV31(openAPI))));
        group.setRequests(toMockRequests(openAPI, valueList));
        group.setSchemas(calcComponentSchemas(openAPI, group));
        return group;
    }

    protected List<ExportRequestVo> toMockRequests(OpenAPI openAPI, List<Triple<String, PathItem, List<Pair<String, Operation>>>> valueList) {
        return valueList.stream().flatMap(triple -> {
            String path = triple.getLeft();
            List<Pair<String, Operation>> operations = triple.getRight();
            return operations.stream().map(operationPair -> {
                String method = operationPair.getKey();
                Operation operation = operationPair.getValue();
                ExportRequestVo requestVo = new ExportRequestVo();
                requestVo.setRequestName(StringUtils.abbreviate(StringUtils.defaultIfBlank(operation.getOperationId(), operation.getSummary()), 200));
                requestVo.setRequestPath(path);
                requestVo.setMethod(StringUtils.upperCase(method));
                requestVo.setDescription(operation.getDescription());
                requestVo.setStatus(1);
                requestVo.setSchemas(getRequestSchemaVo(openAPI, operation));
                requestVo.setDataList(toMockDataList(openAPI, operation, requestVo));
                return requestVo;
            });
        }).collect(Collectors.toList());
    }

    protected String getParametersSchema(OpenAPI openAPI, Operation operation) {
        if (operation.getParameters() != null) {
            return SchemaJsonUtils.toJson(operation.getParameters(), isV31(openAPI));
        }
        return null;
    }

    protected List<ExportSchemaVo> getRequestSchemaVo(OpenAPI openAPI, Operation operation) {
        String parametersSchema = getParametersSchema(openAPI, operation);
        List<ExportSchemaVo> requestSchemas = new ArrayList<>();
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : operation.getRequestBody().getContent().entrySet()) {
                String contentType = entry.getKey();
                io.swagger.v3.oas.models.media.MediaType mediaType = entry.getValue();
                if (mediaType != null && mediaType.getSchema() != null) {
                    ExportSchemaVo exportSchemaVo = new ExportSchemaVo();
                    exportSchemaVo.setParametersSchema(parametersSchema);
                    exportSchemaVo.setRequestMediaType(contentType);
                    exportSchemaVo.setRequestBodySchema(SchemaJsonUtils.toJson(mediaType.getSchema(), isV31(openAPI)));
                    exportSchemaVo.setBodyType(MockConstants.MOCK_SCHEMA_BODY_TYPE_CONTENT);
                    List<Example> requestExamples = getExamples(mediaType);
                    if (!requestExamples.isEmpty()) {
                        exportSchemaVo.setRequestExamples(SchemaJsonUtils.toJson(requestExamples, isV31(openAPI)));
                    }
                    requestSchemas.add(exportSchemaVo);
                }
            }
        } else if (StringUtils.isNotBlank(parametersSchema)) {
            ExportSchemaVo exportSchemaVo = new ExportSchemaVo();
            exportSchemaVo.setParametersSchema(parametersSchema);
            exportSchemaVo.setBodyType(MockConstants.MOCK_SCHEMA_BODY_TYPE_CONTENT);
            requestSchemas.add(exportSchemaVo);
        }
        return requestSchemas;
    }

    protected List<ExportSchemaVo> getResponseSchemaVo(OpenAPI openAPI, ExportRequestVo requestVo, Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry) {
        List<ExportSchemaVo> responseSchemas = new ArrayList<>();
        if (entry != null) {
            ExportSchemaVo requestSchema = null;
            for (ExportSchemaVo reqSchema : requestVo.getSchemas()) {
                if (requestSchema == null || StringUtils.equalsIgnoreCase(reqSchema.getRequestMediaType(), entry.getKey())) {
                    requestSchema = reqSchema;
                }
            }
            responseSchemas.addAll(calcResponseSchemaVo(openAPI, entry, requestSchema));
        }
        return responseSchemas;
    }

    protected List<ExportSchemaVo> calcResponseSchemaVo(OpenAPI openAPI, Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry, ExportSchemaVo requestSchema) {
        List<ExportSchemaVo> dataSchemas = new ArrayList<>();
        String contentType = entry.getKey();
        io.swagger.v3.oas.models.media.MediaType mediaType = entry.getValue();
        if (mediaType != null && mediaType.getSchema() != null) {
            ExportSchemaVo exportSchemaVo = new ExportSchemaVo();
            if (requestSchema != null) {
                copyProperties(exportSchemaVo, requestSchema);
            }
            exportSchemaVo.setResponseMediaType(contentType);
            exportSchemaVo.setResponseBodySchema(SchemaJsonUtils.toJson(mediaType.getSchema(), isV31(openAPI)));
            exportSchemaVo.setBodyType(MockConstants.MOCK_SCHEMA_BODY_TYPE_CONTENT);
            List<Example> responseExamples = getExamples(mediaType);
            if (!responseExamples.isEmpty()) {
                exportSchemaVo.setResponseExamples(SchemaJsonUtils.toJson(responseExamples, isV31(openAPI)));
            }
            dataSchemas.add(exportSchemaVo);
        }
        return dataSchemas;
    }

    protected List<Example> getExamples(io.swagger.v3.oas.models.media.MediaType mediaType){
        List<Example> examples = new ArrayList<>();
        if (mediaType != null) {
            if (mediaType.getExamples() != null) {
                examples.addAll(mediaType.getExamples().values());
            }
            if (mediaType.getExample() != null) {
                examples.add(new Example().summary("Example").value(mediaType.getExample()));
            }
        }
        return examples;
    }

    protected void copyProperties(Object target, Object source) {
        try {
            BeanUtils.copyProperties(target, source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("复制属性错误", e);
        }
    }

    protected List<ExportDataVo> toMockDataList(OpenAPI openAPI, Operation operation, ExportRequestVo requestVo) {
        if (operation.getResponses() != null) {
            return operation.getResponses().entrySet().stream().flatMap(entry -> {
                String statusCode = entry.getKey();
                ExportDataVo dataVo = new ExportDataVo();
                dataVo.setStatus(1);
                if (NumberUtils.isDigits(statusCode)) {
                    dataVo.setStatusCode(Integer.parseInt(statusCode));
                } else {
                    dataVo.setStatusCode(HttpStatus.OK.value());
                }
                dataVo.setContentType(MediaType.APPLICATION_JSON_VALUE);
                dataVo.setDescription(operation.getDescription());
                ApiResponse apiResponse = entry.getValue();
                if (apiResponse != null) {
                    dataVo.setDescription(StringUtils.defaultIfBlank(apiResponse.getDescription(), operation.getDescription()));
                    Content content = apiResponse.getContent();
                    if (content != null && !content.isEmpty()) {
                        return content.entrySet().stream().map(contentEntry -> {
                            ExportDataVo resData = new ExportDataVo();
                            copyProperties(resData, dataVo);
                            String contentType = contentEntry.getKey();
                            resData.setContentType(contentType);
                            io.swagger.v3.oas.models.media.MediaType mediaType = contentEntry.getValue();
                            List<Example> responseExamples = getExamples(mediaType);
                            if (!responseExamples.isEmpty()) {
                                Object exampleValue = responseExamples.get(0).getValue();
                                if(!(exampleValue instanceof String)){
                                    exampleValue = SchemaJsonUtils.toJson(exampleValue, isV31(openAPI));
                                }
                                resData.setResponseBody((String) exampleValue);
                            }
                            resData.setSchemas(getResponseSchemaVo(openAPI, requestVo, contentEntry));
                            return resData;
                        });
                    }
                }
                return Stream.of(dataVo);
            }).collect(Collectors.toList());
        } else {
            ExportDataVo dataVo = new ExportDataVo();
            dataVo.setStatus(1);
            dataVo.setStatusCode(HttpStatus.OK.value());
            dataVo.setContentType(MediaType.APPLICATION_JSON_VALUE);
            dataVo.setDescription(operation.getDescription());
            return List.of(dataVo);
        }
    }

    protected List<Pair<String, Operation>> getAllOperationsInAPath(PathItem pathObj) {
        List<Pair<String, Operation>> operations = new ArrayList<>();
        addToOperationsList(operations, pathObj.getGet(), PathItem.HttpMethod.GET.name());
        addToOperationsList(operations, pathObj.getPut(), PathItem.HttpMethod.PUT.name());
        addToOperationsList(operations, pathObj.getPost(), PathItem.HttpMethod.POST.name());
        addToOperationsList(operations, pathObj.getPatch(), PathItem.HttpMethod.PATCH.name());
        addToOperationsList(operations, pathObj.getDelete(), PathItem.HttpMethod.DELETE.name());
        addToOperationsList(operations, pathObj.getTrace(), PathItem.HttpMethod.TRACE.name());
        addToOperationsList(operations, pathObj.getOptions(), PathItem.HttpMethod.OPTIONS.name());
        addToOperationsList(operations, pathObj.getHead(), PathItem.HttpMethod.HEAD.name());
        return operations;
    }

    private void addToOperationsList(List<Pair<String, Operation>> operationsList, Operation operation, String method) {
        if (operation == null) {
            return;
        }
        operationsList.add(Pair.of(method, operation));
    }

    protected boolean isV31(OpenAPI openAPI) {
        return SpecVersion.V31.equals(openAPI.getSpecVersion());
    }
}
