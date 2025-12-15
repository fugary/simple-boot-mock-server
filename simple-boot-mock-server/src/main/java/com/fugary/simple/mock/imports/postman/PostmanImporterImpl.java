package com.fugary.simple.mock.imports.postman;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.NameValueObj;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import dev.jora.postman4j.models.*;
import dev.jora.postman4j.utils.ConverterUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Create date 2025/12/12<br>
 *
 * @author gary.fu
 */
@Component
@Slf4j
public class PostmanImporterImpl implements MockGroupImporter {

    @Override
    public boolean isSupport(String type) {
        return "postman".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        try {
            List<ExportGroupVo> mockGroups = convert(data);
            ExportMockVo exportMockVo = new ExportMockVo();
            exportMockVo.setGroups(mockGroups);
            return exportMockVo;
        } catch (Exception e) {
            log.error("解析postman collections数据错误", e);
        }
        return null;
    }

    public List<ExportGroupVo> convert(String json) throws Exception {
        PostmanCollection postmanCollection = ConverterUtils.fromJsonString(json);
        List<ExportGroupVo> groups = new ArrayList<>();
        ExportGroupVo rootGroup = null;
        for (Items item : postmanCollection.getItem()) {
            if (CollectionUtils.isEmpty(item.getItem())) { // 根目录
                if (rootGroup == null) {
                    rootGroup = new ExportGroupVo();
                    Information info = postmanCollection.getInfo();
                    rootGroup.setStatus(1);
                    rootGroup.setGroupName(info.getName());
                    rootGroup.setGroupPath(DigestUtils.md5Hex(rootGroup.getGroupName() + "#" + JsonUtils.toJson(postmanCollection.getItem())));
                    rootGroup.setDescription(info.getDescription() != null ? info.getDescription().stringValue : "");
                    rootGroup.setRequests(new ArrayList<>());
                    groups.add(rootGroup);
                }
                parseItemRecursive(item, rootGroup, groups);
            } else {
                parseItemRecursive(item, null, groups);
            }
        }

        return groups;
    }

    /**
     * 递归处理 folder / request
     */
    private void parseItemRecursive(Items item, ExportGroupVo parentGroup, List<ExportGroupVo> groups) {
        if (CollectionUtils.isNotEmpty(item.getItem())) {
            ExportGroupVo group = new ExportGroupVo();
            group.setGroupName(item.getName());
            if (parentGroup != null) {
                group.setGroupName(StringUtils.join(List.of(parentGroup.getGroupName(), item.getName()), "/"));
            }
            group.setGroupPath(DigestUtils.md5Hex(group.getGroupName() + "#" + JsonUtils.toJson(item.getItem())));
            group.setDescription(item.getDescription() != null ? item.getDescription().stringValue : "");
            group.setStatus(1);
            group.setRequests(new ArrayList<>());
            groups.add(group);
            for (Items child : item.getItem()) {
                parseItemRecursive(child, group, groups);
            }
        } else if (item.getRequest() != null) {
            ExportRequestVo requestVo = parseRequest(item, item.getRequest());
            parentGroup.getRequests().add(requestVo);
        }
    }

    /**
     * 解析 request 项
     */
    private ExportRequestVo parseRequest(Items item, RequestUnion requestUnion) {
        RequestClass request = requestUnion.getRequestClassValue();
        ExportRequestVo requestVo = new ExportRequestVo();
        requestVo.setMethod(request.getMethod());
        requestVo.setRequestName(item.getName());
        requestVo.setDescription(item.getDescription() != null ? item.getDescription().stringValue : "");
        requestVo.setRequestPath(getRequestPath(request.getUrl()));
        requestVo.setStatus(1);
        MockParamsVo paramsVo = getMockParamsVo(request);
        requestVo.setMockParams(JsonUtils.toJson(paramsVo));
        // response examples
        requestVo.setDataList(parseMockData(requestVo, item));
        return requestVo;
    }

    private String getRequestPath(URL url) {
        if (url.getUrlClassValue() != null) {
            Optional<URLPath> pathOptional = Optional.ofNullable(url.getUrlClassValue().getPath());
            if (pathOptional.isPresent()) {
                return "/" + StringUtils.join(url.getUrlClassValue().getPath().unionArrayValue, "/");
            }
            return "/";
        } else if (StringUtils.isNotBlank(url.getStringValue())) {
            try {
                return new java.net.URL(url.getStringValue()).getPath();
            } catch (MalformedURLException e) {
                log.error("URL格式不正确", e);
            }
        }
        return null;
    }

    private MockParamsVo getMockParamsVo(RequestClass request) {
        URLClass urlData = request.getUrl().getUrlClassValue();
        MockParamsVo paramsVo = null;
        // body
        Body body = request.getBody();
        if (body != null) {
            paramsVo = getOrCreateMockParamsVo(paramsVo);
            if (body.getRaw() != null) {
                paramsVo.setRequestBody(body.getRaw());
            } else if (body.getUrlencoded() != null) {
                paramsVo.setFormUrlencoded(body.getUrlencoded().stream()
                        .map(encoded -> new NameValue(encoded.getKey(), encoded.getValue()))
                        .collect(Collectors.toList()));
            } else if (body.getFormdata() != null) {
                paramsVo.setFormData(body.getFormdata().stream()
                        .map(formData -> new NameValueObj(formData.getKey(), formData.getValue()))
                        .collect(Collectors.toList()));
            }
        }
        if (urlData != null && CollectionUtils.isNotEmpty(urlData.getQuery())) {
            paramsVo = getOrCreateMockParamsVo(paramsVo);
            paramsVo.setRequestParams(urlData.getQuery().stream()
                    .map(queryParam -> new NameValue(queryParam.getKey(), queryParam.getValue()))
                    .collect(Collectors.toList()));
        }
        List<Header> headers = getReqUnionHeaders(request.getHeader());
        if (CollectionUtils.isNotEmpty(headers)) {
            paramsVo = getOrCreateMockParamsVo(paramsVo);
            paramsVo.setHeaderParams(headers.stream()
                    .map(headerParam -> new NameValue(headerParam.getKey(), headerParam.getValue()))
                    .collect(Collectors.toList()));
        }
        return paramsVo;
    }

    private List<Header> getReqUnionHeaders(HeaderUnion headerUnion) {
        if (headerUnion != null && CollectionUtils.isNotEmpty(headerUnion.getHeaderArrayValue())) {
            return JsonUtils.fromJson(JsonUtils.toJson(headerUnion.getHeaderArrayValue()), new TypeReference<>() {
            });
        }
        return new ArrayList<>();
    }

    private List<HeaderElement> getResUnionHeaders(Headers headerUnion) {
        if (headerUnion != null && CollectionUtils.isNotEmpty(headerUnion.getUnionArrayValue())) {
            return JsonUtils.fromJson(JsonUtils.toJson(headerUnion.getUnionArrayValue()), new TypeReference<>() {
            });
        }
        return new ArrayList<>();
    }

    private MockParamsVo getOrCreateMockParamsVo(MockParamsVo parmaVo) {
        return parmaVo != null ? parmaVo : new MockParamsVo();
    }

    /**
     * 解析 response → ExportDataVo
     */
    private List<ExportDataVo> parseMockData(ExportRequestVo requestVo, Items item) {
        List<ExportDataVo> list = new ArrayList<>();
        if (item.getResponse() == null)
            return list;
        for (Response resp : item.getResponse()) {
            ExportDataVo data = new ExportDataVo();
            data.setDataName(resp.getName());
            if (resp.getCode() != null) {
                data.setStatusCode(resp.getCode().intValue());
            }
            data.setStatus(1);
            data.setResponseBody(resp.getBody());
            data.setMockParams(requestVo.getMockParams());
            // Content-Type
            String contentType = findContentType(getResUnionHeaders(resp.getHeader()));
            data.setContentType(contentType);
            data.setResponseFormat("json");
            if (StringUtils.containsIgnoreCase(contentType, "xml")) {
                data.setResponseFormat("xml");
            }
            list.add(data);
        }

        return list;
    }

    private String findContentType(List<HeaderElement> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return MediaType.APPLICATION_JSON_VALUE;
        }
        return headers.stream()
                .filter(h -> HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(h.headerValue.getKey()))
                .map(h -> h.headerValue.getValue())
                .findFirst()
                .orElse(MediaType.APPLICATION_JSON_VALUE);
    }
}
