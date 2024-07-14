package com.fugary.simple.mock.imports.swagger;

import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SwaggerImporterImpl implements MockGroupImporter {
    @Override
    public boolean isSupport(String type) {
        return "swagger".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        SwaggerParseResult result = new OpenAPIParser().readContents(data, null, null);
        OpenAPI openAPI = result.getOpenAPI();
        if (openAPI != null) {
            Map<String, List<Triple<String, PathItem, List<Pair<String, Operation>>>>> pathMap = openAPI.getPaths().entrySet().stream().map(entry -> {
                PathItem pathItem = entry.getValue();
                List<Pair<String, Operation>> operations = getAllOperationsInAPath(pathItem);
                return Triple.of(entry.getKey(), entry.getValue(), operations);
            }).collect(Collectors.groupingBy(triple -> {
                List<Pair<String, Operation>> operations = triple.getRight();
                Pair<String, Operation> firstOptionPair = operations.get(0);
                return firstOptionPair.getRight().getTags().get(0);
            }));
            List<ExportGroupVo> mockGroups = openAPI.getTags().stream().map(tag -> toMockGroup(tag, openAPI.getInfo(), pathMap.get(tag.getName()))).collect(Collectors.toList());
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
                requestVo.setDataList(toMockDataList(operation));
                return requestVo;
            });
        }).collect(Collectors.toList());
    }

    protected List<ExportDataVo> toMockDataList(Operation operation) {
        if (operation.getResponses() != null) {
            return operation.getResponses().entrySet().stream().map(entry -> {
                String statusCode = entry.getKey();
                ExportDataVo dataVo = new ExportDataVo();
                dataVo.setStatus(1);
                dataVo.setStatusCode(Integer.parseInt(statusCode));
                dataVo.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse apiResponse = entry.getValue();
                dataVo.setDescription(operation.getDescription());
                if (apiResponse != null) {
                    dataVo.setDescription(StringUtils.defaultIfBlank(apiResponse.getDescription(), operation.getDescription()));
                    Content content = apiResponse.getContent();
                    if (content != null) {
                        List<String> keys = new ArrayList<>(content.keySet());
                        if (CollectionUtils.isNotEmpty(keys)) {
                            dataVo.setContentType(keys.get(0));
                            io.swagger.v3.oas.models.media.MediaType mediaType = content.get(keys.get(0));
                            if (mediaType != null && mediaType.getSchema() != null && mediaType.getSchema().getExample() != null) {
                                dataVo.setResponseBody(JsonUtils.toJson(mediaType.getSchema().getExample()));
                            }
                        }
                    }
                }
                return dataVo;
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
