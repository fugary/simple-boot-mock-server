package com.fugary.simple.mock.utils;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.*;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.NameValueObj;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.utils.servlet.HttpRequestUtils.getBodyResource;

/**
 * Created on 2020/5/6 9:06 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleMockUtils {

    /**
     * 混淆密码使用的pattern
     */
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)((password|secret)\\\\?\"):\\s*(\\\\?\")[^\"\\\\]+(\\\\?\")");

    private static final List<String> TEXT_CONTENT_TYPES = List.of(
            "application/json",
            "application/xml",
            "text/html",
            "text/plain",
            "text/css",
            "application/javascript",
            "text/javascript",
            "application/graphql",
            "application/yaml",
            "text/markdown"
    );

    private static final List<String> TEXT_MEDIA_TYPE_KEYWORDS = Arrays.asList(
            "json",
            "*",
            "text"
    );

    /**
     * 转byte数组
     * @param list
     * @return
     */
    public static byte[] listToByteArray(List<?> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj instanceof Number) {
                Number num = (Number) obj;
                bytes[i] = num.byteValue();
            }
        }
        return bytes;
    }

    /**
     * 生成uuid
     *
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * @param target
     * @param <T>
     */
    public static <T extends MockBase> T addAuditInfo(T target) {
        Date currentDate = new Date();
        if (target != null) {
            MockUser loginUser = SecurityUtils.getLoginUser();
            if (target.getId() == null) {
                target.setCreateDate(Objects.requireNonNullElse(target.getCreateDate(), currentDate));
                if (loginUser != null) {
                    target.setCreator(loginUser.getUserName());
                }
            } else {
                target.setModifyDate(currentDate);
                if (loginUser != null) {
                    target.setModifier(loginUser.getUserName());
                }
            }
        }
        return target;
    }

    /**
     * 判断是否是默认响应数据
     *
     * @param mockData
     * @return
     */
    public static boolean isDefault(MockData mockData) {
        return getDefaultFlag(mockData.getDefaultFlag()) == 1;
    }

    /**
     * 计算defaultFlag
     *
     * @param defaultFlag
     * @return
     */
    public static int getDefaultFlag(Integer defaultFlag) {
        return defaultFlag == null || defaultFlag == 0 ? 0 : 1;
    }

    /**
     * mock 预览
     *
     * @param request
     * @return
     */
    public static boolean isMockPreview(HttpServletRequest request) {
        return BooleanUtils.toBoolean(StringUtils
                .defaultIfBlank(request.getHeader(MockConstants.MOCK_DATA_PREVIEW_HEADER), Boolean.FALSE.toString()));
    }

    /**
     * 计算保存为json的头信息
     *
     * @param headers
     * @return
     */
    public static HttpHeaders calcHeaders(String headers) {
        if (StringUtils.isNotBlank(headers)) {
            List<Map<String, String>> headerList = JsonUtils.fromJson(headers, List.class);
            HttpHeaders httpHeaders = new HttpHeaders();
            headerList.stream().forEach(map -> httpHeaders.add(map.get("name"), map.get("value")));
            return httpHeaders;
        }
        return null;
    }

    /**
     * 是否是可用proxyUrl
     * @param proxyUrl
     * @return
     */
    public static boolean isValidProxyUrl(String proxyUrl) {
        return StringUtils.isNotBlank(proxyUrl) && proxyUrl.matches("https?://.+");
    }

    /**
     * 过滤部分header请求
     * @return
     */
    public static Set<String> getExcludeHeaders(){
        List<String> list = Arrays.asList(
            HttpHeaders.HOST.toLowerCase(),
            HttpHeaders.ORIGIN.toLowerCase(),
            HttpHeaders.REFERER.toLowerCase()
        );
        return new HashSet<>(list);
    }

    /**
     * 判断是否是需要过滤
     * @return
     */
    public static boolean isExcludeHeaders(String headerName){
        headerName = StringUtils.trimToEmpty(headerName).toLowerCase();
        return getExcludeHeaders().contains(headerName)
                || headerName.matches("^(sec-|mock-).*");
    }

    /**
     * 清理cors相关的头信息，代理时使用自己的头信息
     * @param response ResponseEntity
     */
    public static <T> ResponseEntity<T> removeProxyHeaders(ResponseEntity<T> response) {
        if (response != null) {
            HttpHeaders headers = new HttpHeaders();
            response.getHeaders().forEach((headerName, value) -> {
                if (!StringUtils.startsWithIgnoreCase(headerName, "access-control-")
                        && !StringUtils.equalsIgnoreCase(HttpHeaders.CONNECTION, headerName)) {
                    headers.addAll(headerName, value);
                }
            });
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        return response;
    }

    /**
     * 解析成MockParamsVo
     *
     * @param mockGroup
     * @param mockRequest
     * @param request
     * @return
     */
    public static MockParamsVo toMockParams(MockGroup mockGroup, MockRequest mockRequest, HttpServletRequest request) {
        MockParamsVo mockParams = new MockParamsVo();
        String pathPrefix = request.getContextPath() + "/mock/\\w+(/.*)";
        String requestPath = request.getRequestURI();
        Matcher matcher = Pattern.compile(pathPrefix).matcher(requestPath);
        if (matcher.matches()) {
            requestPath = matcher.group(1);
        }
        String proxyUrl = calcProxyUrl(mockGroup, mockRequest);
        mockParams.setTargetUrl(proxyUrl);
        mockParams.setRequestPath(requestPath);
        mockParams.setMethod(request.getMethod());
        Enumeration<String> headerNames = request.getHeaderNames();
        List<NameValue> headers = mockParams.getHeaderParams();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            boolean excludeHeader = SimpleMockUtils.getExcludeHeaders().contains(headerName.toLowerCase());
            if (SimpleMockUtils.isMockPreview(request)) {
                excludeHeader = SimpleMockUtils.isExcludeHeaders(headerName.toLowerCase());
            }
            if (!excludeHeader && StringUtils.isNotBlank(headerValue)) {
                headers.add(new NameValue(headerName, headerValue));
            }
        }
        headers.add(new NameValue(MockConstants.SIMPLE_BOOT_MOCK_HEADER, "1"));
        Enumeration<String> parameterNames = request.getParameterNames();
        List<NameValue> parameters = mockParams.getRequestParams();
        List<NameValue> formUrlencoded = mockParams.getFormUrlencoded();
        List<NameValueObj> formData = mockParams.getFormData();
        boolean isUrlencoded = HttpRequestUtils.isCompatibleWith(request, MediaType.APPLICATION_FORM_URLENCODED);
        boolean isFormData = HttpRequestUtils.isCompatibleWith(request, MediaType.MULTIPART_FORM_DATA);
        if (isFormData) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            multipartRequest.getFileNames().forEachRemaining(fieldName -> {
                formData.add(new NameValueObj(fieldName, multipartRequest.getFiles(fieldName)));
            });
            multipartRequest.getParameterMap().keySet().forEach(paramName -> {
                String paramValue = multipartRequest.getParameter(paramName);
                if (StringUtils.isNotBlank(paramValue)) {
                    formData.add(new NameValueObj(paramName, paramValue));
                }
            });
        } else if (isUrlencoded) {
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String parameterValue = request.getParameter(parameterName);
                if (StringUtils.isNotBlank(parameterValue)) {
                    formUrlencoded.add(new NameValue(parameterName, parameterValue));
                }
            }
        } else {
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String parameterValue = request.getParameter(parameterName);
                if (StringUtils.isNotBlank(parameterValue)) {
                    parameters.add(new NameValue(parameterName, parameterValue));
                }
            }
        }
        mockParams.setContentType(request.getContentType());
        try {
            mockParams.setRequestBody(StreamUtils.copyToString(getBodyResource(request).getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Body解析错误", e);
        }
        return mockParams;
    }

    /**
     * 计算Proxy地址
     *
     * @param mockGroup
     * @param mockRequest
     * @return
     */
    public static String calcProxyUrl(MockGroup mockGroup, MockRequest mockRequest) {
        String proxyUrl = mockGroup.getProxyUrl();
        if (mockRequest != null && StringUtils.isNotBlank(mockRequest.getProxyUrl())) {
            proxyUrl = mockRequest.getProxyUrl();
        }
        return proxyUrl;
    }

    /**
     * content type 计算
     *
     * @param mockGroup
     * @param mockRequest
     * @param mockData
     * @return
     */
    public static String calcContentType(MockGroup mockGroup, MockRequest mockRequest, MockData mockData) {
        if (mockData != null && StringUtils.isNotBlank(mockData.getContentType())) {
            return mockData.getContentType();
        }
        if (mockRequest != null && StringUtils.isNotBlank(mockRequest.getContentType())) {
            return mockRequest.getContentType();
        }
        if (mockGroup != null && StringUtils.isNotBlank(mockGroup.getContentType())) {
            return mockGroup.getContentType();
        }
        return null;
    }

    /**
     * 获取上传文件信息
     *
     * @param request
     * @return
     */
    public static List<MultipartFile> getUploadFiles(MultipartHttpServletRequest request) {
        List<MultipartFile> files = request.getFiles("files");
        if (CollectionUtils.isEmpty(files)) {
            files = request.getFiles("file");
        }
        return files;
    }

    /**
     * 处理参数解析为String，并且特殊处理密码混淆
     * @param argsList
     * @return
     */
    public static String logDataString(List<Object> argsList) {
        String resultStr = StringUtils.EMPTY;
        if (argsList.size() == 1) {
            Object arg = argsList.get(0);
            resultStr = (arg.getClass().isPrimitive() || arg instanceof String) ? arg.toString() : JsonUtils.toJson(arg);
        } else if (argsList.size() > 1) {
            resultStr = JsonUtils.toJson(argsList);
        }
        resultStr = PASSWORD_PATTERN.matcher(resultStr).replaceAll("$1:$3***$4");
        return resultStr;
    }

    public static String calcMockSchemaKey(Integer groupId, Integer requestId, Integer dataId) {
        return StringUtils.join(groupId, "-", requestId, "-", dataId);
    }

    /**
     * 复制属性
     *
     * @param from
     * @param to
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T, S> T copy(S from, T to) {
        try {
            BeanUtils.copyProperties(from, to);
        } catch (Exception e) {
            log.error("copy属性错误", e);
        }
        return to;
    }

    /**
     * 复制属性
     *
     * @param from
     * @param to
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T, S> T copy(S from, Class<T> to) {
        if (from == null) {
            return null;
        }
        Constructor<T> constructor = null;
        T target = null;
        try {
            constructor = to.getConstructor();
            target = constructor.newInstance();
            copy(from, target);
        } catch (Exception e) {
            log.error("copy属性错误", e);
        }
        return target;
    }

    /**
     * 计算ContentType
     *
     * @param contentType
     * @param charset
     * @return
     */
    public static String getContentType(String contentType, String charset) {
        charset = StringUtils.defaultIfBlank(charset, StandardCharsets.UTF_8.name());
        if (StringUtils.equalsIgnoreCase(charset, "none") || isStreamContentType(contentType)) {
            return contentType;
        }
        return contentType + ";charset=" + charset;
    }

    /**
     * 是否是流mediaType
     *
     * @param mediaType
     * @return
     */
    public static boolean isStreamMediaType(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        String type = mediaType.getType().toLowerCase();
        String subtype = mediaType.getSubtype().toLowerCase();
        String fullType = type + "/" + subtype;
        if (TEXT_MEDIA_TYPE_KEYWORDS.stream().anyMatch(fullType::contains)
                || TEXT_CONTENT_TYPES.stream().anyMatch(fullType::contains)) {
            return false;
        }
        // 检查是否为流式 MediaType
        return true;
    }

    /**
     * 是否是流contentType
     *
     * @param contentType
     * @return
     */
    public static boolean isStreamContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return isStreamMediaType(mediaType);
        } catch (Exception e) {
            return false; // 无效的 Content-Type
        }
    }

    /**
     * base64部分提取
     *
     * @param input
     * @return
     */
    public static String extractBase64Data(String input) {
        input = StringUtils.trimToEmpty(input);
        if (StringUtils.isBlank(input)) {
            return input;
        }
        // 检查是否为 Data URL（以 "data:" 开头）
        if (input.startsWith("data:")) {
            int commaIndex = input.indexOf(',');
            if (commaIndex == -1) {
                return null; // 无效的 Data URL
            }
            return input.substring(commaIndex + 1);
        }
        // 如果没有前缀，假设整个输入是 Base64 数据
        return input;
    }

    /**
     * 解析base64内容
     * @param responseBody
     * @return
     */
    public static byte[] getStreamResponseBody(String responseBody){
        responseBody = extractBase64Data(responseBody);
        try {
            return Base64.getDecoder().decode(responseBody);
        } catch (Exception e) {
            log.error("解码失败", e);
        }
        return null;
    }

    /**
     * body处理
     *
     * @param data
     * @return
     */
    public static Pair<String, Object> getMockResponseBody(MockData data, String contentType) {
        Object result = data.getResponseBody();
        contentType = SimpleMockUtils.getContentType(contentType, data.getDefaultCharset());
        if (SimpleMockUtils.isStreamContentType(contentType)) {
            byte[] streamResponseBody = SimpleMockUtils.getStreamResponseBody(data.getResponseBody());
            if (streamResponseBody != null) {
                result = streamResponseBody;
            } else {
                contentType = ContentType.TEXT_PLAIN.getMimeType();
            }
        }
        return Pair.of(contentType, result);
    }

    /**
     * 是否没有改变
     *
     * @param oldMockData
     * @param newMockData
     * @return
     */
    public static boolean isSameMockData(MockBase oldMockData, MockBase newMockData) {
        return EqualsBuilder.reflectionEquals(oldMockData, newMockData, "version", "modifyFrom",
                MockConstants.CREATOR_KEY, MockConstants.CREATE_DATE_KEY,
                MockConstants.MODIFIER_KEY, MockConstants.MODIFY_DATE_KEY);
    }

    /**
     * 从历史数据恢复
     *
     * @param history
     * @param target
     */
    public static void copyFromHistory(HistoryBase history, HistoryBase target) {
        HistoryBase tmpTarget = copy(target, target.getClass()); // 暂存
        copy(history, target); // 用历史版本覆盖
        target.setId(tmpTarget.getId()); // 还原不能修改的属性
        target.setVersion(tmpTarget.getVersion());
        target.setModifyFrom(tmpTarget.getModifyFrom());
    }

    /**
     * 数据相加
     *
     * @param mockVo1
     * @param mockVo2
     */
    public static void addMockVo(ExportMockVo mockVo1, ExportMockVo mockVo2) {
        if (mockVo1.getGroups() == null) {
            mockVo1.setGroups(new ArrayList<>());
        }
        mockVo1.getGroups().addAll(mockVo2.getGroups() == null ? new ArrayList<>() : mockVo2.getGroups());
    }

    /**
     * 策略合并
     *
     * @param mockVo
     * @param duplicateStrategy
     */
    public static SimpleResult<List<ExportGroupVo>> mergeExportMockVo(ExportMockVo mockVo, Integer duplicateStrategy) {
        List<ExportGroupVo> groups = mockVo.getGroups();
        if (CollectionUtils.isNotEmpty(groups)) {
            Map<String, List<ExportGroupVo>> groupsMap = groups.stream().filter(group -> StringUtils.isNotBlank(group.getGroupPath()))
                    .collect(Collectors.groupingBy(ExportGroupVo::getGroupPath));
            if (!groupsMap.isEmpty()) {
                if (MockConstants.IMPORT_STRATEGY_ERROR.equals(duplicateStrategy)) {
                    if (groupsMap.values().stream().anyMatch(list -> list.size() > 1)) {
                        return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2004);
                    }
                } else if (MockConstants.IMPORT_STRATEGY_SKIP.equals(duplicateStrategy)) {
                    groupsMap.forEach((key, value) -> {
                        if (value.size() > 1) {
                            for (int i = 1; i < value.size(); i++) {
                                groups.remove(value.get(i));
                            }
                        }
                    });
                } else if (MockConstants.IMPORT_STRATEGY_NEW.equals(duplicateStrategy)) {
                    groupsMap.forEach((key, value) -> {
                        if (value.size() > 1) {
                            for (int i = 1; i < value.size(); i++) {
                                value.get(i).setGroupPath(SimpleMockUtils.uuid());
                            }
                        }
                    });
                }
            }
        }
        return SimpleResultUtils.createSimpleResult(groups);
    }

    /**
     * 获取执行的Value结果
     *
     * @param value
     * @param timeout
     * @return
     * @throws ScriptException
     */
    public static Object executeValue(Value value, long timeout) throws ScriptException {
        // 判断是否是 Promise
        if (value.canInvokeMember("then")) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            value.invokeMember("then", (ProxyExecutable) (args) -> {
                future.complete(args[0].as(Object.class));
                return null;
            }, (ProxyExecutable) (args) -> {
                future.completeExceptionally(new ScriptException(args[0].toString()));
                return null;
            });
            try {
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("执行中断异常", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                if (e.getCause() instanceof ScriptException) {
                    throw (ScriptException) e.getCause();
                }
                throw new ScriptException((Exception) e.getCause());
            }
        }
        return value.as(Object.class);
    }
}
