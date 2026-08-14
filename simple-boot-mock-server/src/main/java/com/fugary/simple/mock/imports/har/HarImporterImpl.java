package com.fugary.simple.mock.imports.har;

import com.fugary.simple.mock.entity.mock.MockBase;
import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.NameValueObj;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderMode;
import de.sstoehr.harreader.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HAR (HTTP Archive) 导入器
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class HarImporterImpl implements MockGroupImporter {

    public static final String HAR_TYPE = "har";
    public static final int MAX_SAFE_HEADERS_LENGTH = 1800;

    /**
     * 忽略无用的客户端/传输请求头（使用前缀过滤更多的 sec-* 和 if-* 头）
     */
    private static final Set<String> IGNORED_REQ_HEADERS = Set.of(
            "host", "connection", "accept-encoding", "content-length", "origin", "referer", "priority", "te"
    );

    /**
     * 忽略无用或有害的传输/安全/缓存响应头（CORS/COOP等使用前缀过滤）
     */
    private static final Set<String> IGNORED_RES_HEADERS = Set.of(
            // 传输与连接
            "connection", "content-length", "transfer-encoding", "date", "keep-alive", "server", "content-encoding",
            // 缓存策略
            "cache-control", "expires", "pragma", "etag", "last-modified", "age", "vary",
            // 安全策略与监控
            "strict-transport-security", "x-content-type-options", "x-frame-options", "x-xss-protection",
            "x-download-options", "x-permitted-cross-domain-policies", "permissions-policy", "feature-policy",
            "report-to", "nel", "alt-svc", "timing-allow-origin", "server-timing", "x-powered-by", "set-cookie"
    );

    /**
     * 判断是否忽略该请求头
     */
    private boolean isIgnoredReqHeader(String headerName) {
        if (StringUtils.isBlank(headerName)) {
            return true;
        }
        String lowerName = headerName.toLowerCase();
        if (lowerName.startsWith("sec-") || lowerName.startsWith("if-")) {
            return true;
        }
        return IGNORED_REQ_HEADERS.contains(lowerName);
    }

    /**
     * 判断是否忽略该响应头
     */
    private boolean isIgnoredResHeader(String headerName) {
        if (StringUtils.isBlank(headerName)) {
            return true;
        }
        String lowerName = headerName.toLowerCase();
        if (lowerName.startsWith("access-control-") || lowerName.startsWith("cross-origin-") || lowerName.startsWith("content-security-policy")) {
            return true;
        }
        return IGNORED_RES_HEADERS.contains(lowerName);
    }

    @Override
    public boolean isSupport(String type) {
        return HAR_TYPE.equalsIgnoreCase(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        try {
            HarReader harReader = new HarReader();
            Har har = harReader.readFromString(data, HarReaderMode.LAX);
            if (har == null || har.getLog() == null || CollectionUtils.isEmpty(har.getLog().getEntries())) {
                log.warn("HAR数据中没有有效entries");
                return null;
            }
            List<HarEntry> validEntries = har.getLog().getEntries().stream()
                    .filter(this::isValidApiEntry)
                    .collect(Collectors.toList());

            if (validEntries.isEmpty()) {
                log.warn("HAR数据中未过滤出有效API entries");
                return null;
            }

            Map<String, List<HarEntry>> hostEntriesMap = validEntries.stream()
                    .collect(Collectors.groupingBy(this::resolveHost, LinkedHashMap::new, Collectors.toList()));

            List<ExportGroupVo> mockGroups = hostEntriesMap.entrySet().stream()
                    .map(entry -> toMockGroup(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            ExportMockVo exportMockVo = new ExportMockVo();
            exportMockVo.setGroups(mockGroups);
            return exportMockVo;
        } catch (Exception e) {
            log.error("HAR解析失败", e);
        }
        return null;
    }

    /**
     * 判断是否为有效 API 请求（过滤非 HTTP/HTTPS 协议及异常 URL）
     */
    protected boolean isValidApiEntry(HarEntry entry) {
        if (entry == null || entry.getRequest() == null || StringUtils.isBlank(entry.getRequest().getUrl())) {
            return false;
        }
        String urlStr = entry.getRequest().getUrl().trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            return false;
        }
        try {
            new URL(urlStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Host 作为分组键
     */
    protected String resolveHost(HarEntry entry) {
        try {
            URL url = new URL(entry.getRequest().getUrl());
            String host = url.getHost();
            int port = url.getPort();
            if (port > 0 && port != 80 && port != 443) {
                return host + ":" + port;
            }
            return StringUtils.defaultIfBlank(host, "default");
        } catch (Exception e) {
            return "default";
        }
    }

    /**
     * 解析成 MockGroup
     */
    protected ExportGroupVo toMockGroup(String host, List<HarEntry> entryList) {
        ExportGroupVo group = new ExportGroupVo();
        group.setGroupName("HAR-" + host);
        group.setGroupPath(DigestUtils.md5Hex(host));
        group.setStatus(1);
        group.setDescription("Imported from HAR (" + host + ")");
        if (CollectionUtils.isNotEmpty(entryList)) {
            parseBaseFields(group, entryList.get(0));
        }
        group.setRequests(toMockRequests(entryList));
        return group;
    }

    /**
     * 解析 Request 列表，支持同一 (Method + Path) 聚合
     */
    protected List<ExportRequestVo> toMockRequests(List<HarEntry> entryList) {
        // 按 Method + Path 聚合
        Map<String, List<HarEntry>> requestMap = entryList.stream()
                .collect(Collectors.groupingBy(this::resolveRequestMethodAndPath, LinkedHashMap::new, Collectors.toList()));

        return requestMap.values().stream().map(sameEndpointEntries -> {
            HarEntry firstEntry = sameEndpointEntries.get(0);
            HarRequest request = firstEntry.getRequest();
            ExportRequestVo requestVo = new ExportRequestVo();

            String path = resolvePath(request.getUrl());
            requestVo.setRequestPath(path);
            requestVo.setRequestName(StringUtils.trimToNull(firstEntry.getComment()));
            requestVo.setMethod(request.getMethod() != null ? request.getMethod().name().toUpperCase() : "GET");
            requestVo.setStatus(1);
            parseBaseFields(requestVo, firstEntry);

            MockParamsVo paramsVo = buildMockParamsVo(request);
            if (paramsVo != null) {
                requestVo.setMockParams(JsonUtils.toJson(paramsVo));
            }

            List<ExportDataVo> dataList = new ArrayList<>();
            for (int i = 0; i < sameEndpointEntries.size(); i++) {
                HarEntry entry = sameEndpointEntries.get(i);
                ExportDataVo dataVo = toMockData(entry, requestVo, i);
                if (dataVo != null) {
                    dataList.add(dataVo);
                }
            }
            requestVo.setDataList(dataList);
            return requestVo;
        }).collect(Collectors.toList());
    }

    /**
     * 生成分组聚合键 Method:Path
     */
    protected String resolveRequestMethodAndPath(HarEntry entry) {
        String method = entry.getRequest().getMethod() != null ? entry.getRequest().getMethod().name().toUpperCase() : "GET";
        String path = resolvePath(entry.getRequest().getUrl());
        return method + ":" + path;
    }

    /**
     * 提取 URL Path
     */
    protected String resolvePath(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String path = url.getPath();
            return StringUtils.isNotBlank(path) ? path : "/";
        } catch (Exception e) {
            return "/";
        }
    }

    /**
     * 构造请求参数 MockParamsVo
     */
    protected MockParamsVo buildMockParamsVo(HarRequest request) {
        MockParamsVo paramsVo = new MockParamsVo();
        boolean hasContent = false;

        // Query 参数
        if (CollectionUtils.isNotEmpty(request.getQueryString())) {
            List<NameValue> queryParams = request.getQueryString().stream()
                    .filter(q -> StringUtils.isNotBlank(q.getName()))
                    .map(q -> new NameValue(q.getName(), q.getValue()))
                    .collect(Collectors.toList());
            paramsVo.setRequestParams(queryParams);
            hasContent = true;
        }

        // Header 参数（过滤 HTTP/2 伪头和无用传输头）
        if (CollectionUtils.isNotEmpty(request.getHeaders())) {
            List<NameValue> headerParams = request.getHeaders().stream()
                    .filter(h -> StringUtils.isNotBlank(h.getName()) && !h.getName().startsWith(":")
                            && !isIgnoredReqHeader(h.getName()))
                    .map(h -> new NameValue(h.getName(), h.getValue()))
                    .collect(Collectors.toList());
            if (!headerParams.isEmpty()) {
                paramsVo.setHeaderParams(headerParams);
                hasContent = true;
            }
        }

        // Body 参数
        HarPostData postData = request.getPostData();
        if (postData != null) {
            String mimeType = postData.getMimeType();
            String mimeLower = StringUtils.defaultString(mimeType).toLowerCase();
            if (mimeLower.contains("application/x-www-form-urlencoded") && CollectionUtils.isNotEmpty(postData.getParams())) {
                List<NameValue> formUrlencoded = postData.getParams().stream()
                        .map(p -> new NameValue(p.getName(), p.getValue()))
                        .collect(Collectors.toList());
                paramsVo.setFormUrlencoded(formUrlencoded);
                paramsVo.setRequestFormat("form-urlencoded");
                hasContent = true;
            } else if (mimeLower.contains("multipart/form-data") && CollectionUtils.isNotEmpty(postData.getParams())) {
                List<NameValueObj> formData = postData.getParams().stream()
                        .map(p -> new NameValueObj(p.getName(), p.getValue()))
                        .collect(Collectors.toList());
                paramsVo.setFormData(formData);
                paramsVo.setRequestFormat("form-data");
                hasContent = true;
            } else if (StringUtils.isNotBlank(postData.getText())) {
                paramsVo.setRequestBody(postData.getText());
                paramsVo.setRequestFormat(resolveFormatFromMime(mimeType));
                hasContent = true;
            }
        }

        return hasContent ? paramsVo : null;
    }

    /**
     * 解析单个 Response 为 ExportDataVo
     */
    protected ExportDataVo toMockData(HarEntry entry, ExportRequestVo requestVo, int index) {
        HarResponse response = entry.getResponse();
        if (response == null) {
            return null;
        }
        ExportDataVo dataVo = new ExportDataVo();
        parseBaseFields(dataVo, entry);
        dataVo.setStatus(1);

        int status = response.getStatus() > 0 ? response.getStatus() : HttpStatus.OK.value();
        dataVo.setStatusCode(status);
        dataVo.setDataName(requestVo.getMethod() + " - " + status + (index > 0 ? " (" + (index + 1) + ")" : ""));
        dataVo.setMockParams(requestVo.getMockParams());

        // Response Body & ContentType
        HarContent content = response.getContent();
        String contentType = MediaType.APPLICATION_JSON_VALUE;
        String responseBody = "";

        if (content != null) {
            if (StringUtils.isNotBlank(content.getMimeType())) {
                contentType = content.getMimeType();
            }
            String rawText = content.getText();
            if (StringUtils.isNotBlank(rawText)) {
                if ("base64".equalsIgnoreCase(content.getEncoding())) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(rawText);
                        responseBody = new String(decoded, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        responseBody = rawText;
                    }
                } else {
                    responseBody = rawText;
                }
            }
        }

        dataVo.setContentType(contentType);
        dataVo.setResponseFormat(resolveFormatFromMime(contentType));
        dataVo.setResponseBody(responseBody);

        // Response Headers（带深度过滤与安全长度截断保护，避免超出数据库 varchar 限制）
        if (CollectionUtils.isNotEmpty(response.getHeaders())) {
            List<NameValue> resHeaders = response.getHeaders().stream()
                    .filter(h -> StringUtils.isNotBlank(h.getName()) && !h.getName().startsWith(":")
                            && !isIgnoredResHeader(h.getName())
                            && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(h.getName()))
                    .map(h -> new NameValue(h.getName(), h.getValue()))
                    .collect(Collectors.toList());
            String headersJson = buildSafeHeadersJson(resHeaders);
            if (headersJson != null) {
                dataVo.setHeaders(headersJson);
            }
        }

        return dataVo;
    }

    /**
     * 构建安全的响应头 JSON，防止超过数据库字段长度上限
     */
    protected String buildSafeHeadersJson(List<NameValue> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return null;
        }
        List<NameValue> safeList = new ArrayList<>();
        for (NameValue header : headers) {
            safeList.add(header);
            String json = JsonUtils.toJson(safeList);
            if (json != null && json.length() > MAX_SAFE_HEADERS_LENGTH) {
                safeList.remove(safeList.size() - 1);
                break;
            }
        }
        return safeList.isEmpty() ? null : JsonUtils.toJson(safeList);
    }

    /**
     * 根据 MIME Type 识别 response/request format
     */
    protected String resolveFormatFromMime(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return "json";
        }
        String lower = mimeType.toLowerCase();
        if (lower.contains("json")) {
            return "json";
        } else if (lower.contains("xml")) {
            return "xml";
        } else if (lower.contains("html")) {
            return "html";
        }
        return "text";
    }

    /**
     * 基础字段赋值
     */
    protected void parseBaseFields(MockBase base, HarEntry entry) {
        if (entry.getStartedDateTime() != null) {
            base.setCreateDate(entry.getStartedDateTime());
            base.setModifyDate(entry.getStartedDateTime());
        } else {
            Date now = new Date();
            base.setCreateDate(now);
            base.setModifyDate(now);
        }
    }
}
