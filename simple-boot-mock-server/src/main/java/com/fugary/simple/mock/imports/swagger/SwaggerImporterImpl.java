package com.fugary.simple.mock.imports.swagger;

import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.*;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class SwaggerImporterImpl implements MockGroupImporter {
    @Override
    public boolean isSupport(String type) {
        return "swagger".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveRequestBody(true);
        parseOptions.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIParser().readContents(data, null, parseOptions);
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI != null && openAPI.getTags() != null) {
            Map<String, List<Triple<String, PathItem, List<Pair<String, Operation>>>>> pathMap = openAPI.getPaths().entrySet().stream().map(entry -> {
                PathItem pathItem = entry.getValue();
                List<Pair<String, Operation>> operations = getAllOperationsInAPath(pathItem);
                return Triple.of(entry.getKey(), entry.getValue(), operations);
            }).collect(Collectors.groupingBy(triple -> {
                List<Pair<String, Operation>> operations = triple.getRight();
                Pair<String, Operation> firstOptionPair = operations.get(0);
                return firstOptionPair.getRight().getTags().get(0);
            }));
            List<ExportGroupVo> mockGroups = openAPI.getTags().stream().filter(tag -> pathMap.get(tag.getName()) != null)
                    .map(tag -> toMockGroup(tag, openAPI.getInfo(), pathMap.get(tag.getName()))).collect(Collectors.toList());
            ExportMockVo exportMockVo = new ExportMockVo();
            exportMockVo.setGroups(mockGroups);
            return exportMockVo;
        }
        return null;
    }

    /**
     * 解析成MockGroup
     *
     * @param tag
     * @param info
     * @param valueList
     * @return
     */
    protected ExportGroupVo toMockGroup(Tag tag, Info info, List<Triple<String, PathItem, List<Pair<String, Operation>>>> valueList) {
        ExportGroupVo group = new ExportGroupVo();
        group.setGroupName(info.getTitle() + "-" + tag.getName());
        group.setDescription(tag.getDescription());
        group.setStatus(1);
        group.setGroupPath(DigestUtils.md5Hex(JsonUtils.toJson(group)));
        group.setRequests(toMockRequests(valueList));
        return group;
    }

    protected List<ExportRequestVo> toMockRequests(List<Triple<String, PathItem, List<Pair<String, Operation>>>> valueList) {
        return valueList.stream().flatMap(triple -> {
            String path = triple.getLeft();
            PathItem pathItem = triple.getMiddle();
            List<Pair<String, Operation>> operations = triple.getRight();
            return operations.stream().map(operationPair -> {
                String method = operationPair.getKey();
                Operation operation = operationPair.getValue();
                ExportRequestVo requestVo = new ExportRequestVo();
                requestVo.setRequestName(StringUtils.defaultIfBlank(operation.getOperationId(), operation.getSummary()));
                requestVo.setRequestPath(path);
                requestVo.setMethod(StringUtils.upperCase(method));
                requestVo.setDescription(operation.getDescription());
                requestVo.setStatus(1);
                requestVo.setSchemas(getRequestSchemaVo(operation));
                requestVo.setDataList(toMockDataList(operation, requestVo));
                return requestVo;
            });
        }).collect(Collectors.toList());
    }

    protected String getParametersSchema(Operation operation) {
        if (operation.getParameters() != null) {
            return JsonUtils.toJson(operation.getParameters());
        }
        return null;
    }

    protected List<ExportSchemaVo> getRequestSchemaVo(Operation operation) {
        String parametersSchema = getParametersSchema(operation);
        List<ExportSchemaVo> requestSchemas = new ArrayList<>();
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            for (Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry : operation.getRequestBody().getContent().entrySet()) {
                String mediaType = entry.getKey();
                if (entry.getValue() != null && entry.getValue().getSchema() != null) {
                    ExportSchemaVo exportSchemaVo = new ExportSchemaVo();
                    exportSchemaVo.setParametersSchema(parametersSchema);
                    exportSchemaVo.setRequestMediaType(mediaType);
                    exportSchemaVo.setRequestBodySchema(JsonUtils.toJson(entry.getValue().getSchema()));
                    requestSchemas.add(exportSchemaVo);
                }
            }
        }
        return requestSchemas;
    }

    protected List<ExportSchemaVo> getResponseSchemaVo(ExportRequestVo requestVo, Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry) {
        List<ExportSchemaVo> responseSchemas = new ArrayList<>();
        if (entry != null) {
            if (requestVo.getSchemas().isEmpty()) {
                responseSchemas = calcResponseSchemaVo(entry, null);
            } else {
                for (ExportSchemaVo requestSchema : requestVo.getSchemas()) {
                    responseSchemas.addAll(calcResponseSchemaVo(entry, requestSchema));
                }
            }
        }
        return responseSchemas;
    }

    protected List<ExportSchemaVo> calcResponseSchemaVo(Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> entry, ExportSchemaVo requestSchema) {
        List<ExportSchemaVo> dataSchemas = new ArrayList<>();
        String mediaType = entry.getKey();
        if (entry.getValue() != null && entry.getValue().getSchema() != null) {
            ExportSchemaVo exportSchemaVo = new ExportSchemaVo();
            if (requestSchema != null) {
                copyProperties(exportSchemaVo, requestSchema);
            }
            exportSchemaVo.setResponseMediaType(mediaType);
            exportSchemaVo.setResponseBodySchema(JsonUtils.toJson(entry.getValue().getSchema()));
            dataSchemas.add(exportSchemaVo);
        }
        return dataSchemas;
    }

    protected void copyProperties(Object target, Object source) {
        try {
            BeanUtils.copyProperties(target, source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("复制属性错误", e);
        }
    }

    protected List<ExportDataVo> toMockDataList(Operation operation, ExportRequestVo requestVo) {
        if (operation.getResponses() != null) {
            return operation.getResponses().entrySet().stream().flatMap(entry -> {
                String statusCode = entry.getKey();
                ExportDataVo dataVo = new ExportDataVo();
                dataVo.setStatus(1);
                dataVo.setStatusCode(Integer.parseInt(statusCode));
                dataVo.setContentType(MediaType.APPLICATION_JSON_VALUE);
                dataVo.setDescription(operation.getDescription());
                ApiResponse apiResponse = entry.getValue();
                if (apiResponse != null) {
                    dataVo.setDescription(StringUtils.defaultIfBlank(apiResponse.getDescription(), operation.getDescription()));
                    Content content = apiResponse.getContent();
                    if (content != null) {
                        return content.entrySet().stream().map(contentEntry -> {
                            ExportDataVo resData = new ExportDataVo();
                            copyProperties(resData, dataVo);
                            String contentType = contentEntry.getKey();
                            resData.setContentType(contentType);
                            io.swagger.v3.oas.models.media.MediaType mediaType = contentEntry.getValue();
                            if (mediaType != null && mediaType.getSchema() != null && mediaType.getSchema().getExample() != null) {
                                resData.setResponseBody(JsonUtils.toJson(mediaType.getSchema().getExample()));
                            }
                            resData.setSchemas(getResponseSchemaVo(requestVo, contentEntry));
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
}
