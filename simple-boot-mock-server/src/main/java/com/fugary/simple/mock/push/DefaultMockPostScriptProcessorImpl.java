package com.fugary.simple.mock.push;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpResponseVo;
import com.fugary.simple.mock.web.vo.http.MockHeaderVo;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Create date 2026/1/9<br>
 *
 * @author gary.fu
 */
@Component
public class DefaultMockPostScriptProcessorImpl implements MockPostScriptProcessor {

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

    @Override
    public String process(MockRequest mockRequest, MockData mockData, String responseBody) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, mockData);
        if (StringUtils.isNotBlank(postProcessor)) {
            HttpResponseVo responseVo = createResponseVo(mockData == null ? null : mockData.getStatusCode(),
                    responseBody);
            MockJsUtils.setCurrentResponseVo(responseVo);
            try {
                Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
                overrideResponse(mockData, resultPair.getValue());
                responseBody = StringUtils.defaultString(resultPair.getKey());
            } finally {
                MockJsUtils.removeCurrentResponseVo();
            }
        }
        return responseBody;
    }

    @Override
    public ResponseEntity<?> process(MockRequest mockRequest, ResponseEntity<?> responseEntity) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, null);
        if (StringUtils.isNotBlank(postProcessor)) {
            Object body = responseEntity.getBody();
            String responseBody = StringUtils.EMPTY;
            if (body instanceof byte[]) {
                responseBody = new String((byte[]) body, StandardCharsets.UTF_8);
            } else if (body instanceof String) {
                responseBody = (String) body;
            }
            HttpResponseVo responseVo = createResponseVo(responseEntity.getStatusCode().value(), responseBody);
            MockJsUtils.setCurrentResponseVo(responseVo);
            try {
                Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
                responseBody = StringUtils.defaultString(resultPair.getKey());
                byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                responseEntity = overrideResponse(responseEntity, bodyBytes, resultPair.getValue());
            } finally {
                MockJsUtils.removeCurrentResponseVo();
            }
        }
        return responseEntity;
    }

    protected void overrideResponse(MockData mockData, HttpResponseVo httpResponse) {
        if (mockData != null && httpResponse != null) {
            mockData.setStatusCode(httpResponse.getStatusCode());
            if (MapUtils.isNotEmpty(httpResponse.getHeaders())) {
                String headers = StringUtils.defaultIfBlank(mockData.getHeaders(), "[]");
                List<MockHeaderVo> headerList = JsonUtils.fromJson(headers, new TypeReference<>() {
                });
                httpResponse.getHeaders().forEach((k, v) -> {
                    if (StringUtils.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE, k)) {
                        mockData.setContentType(v);
                    }
                    // remove exists header
                    headerList.removeIf(headerVo -> StringUtils.equalsIgnoreCase(headerVo.getName(), k));
                    headerList.add(new MockHeaderVo(true, k, v));
                });
                mockData.setHeaders(JsonUtils.toJson(headerList));
            }
        }
    }

    protected ResponseEntity<?> overrideResponse(ResponseEntity<?> responseEntity, byte[] bodyBytes,
            HttpResponseVo httpResponse) {
        if (httpResponse != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.addAll(responseEntity.getHeaders()); // copy old headers
            if (MapUtils.isNotEmpty(httpResponse.getHeaders())) {
                httpResponse.getHeaders().forEach(headers::set); // override headers
            }
            return ResponseEntity
                    .status(httpResponse.getStatusCode())
                    .headers(headers)
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        } else {
            return ResponseEntity
                    .status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        }
    }

    protected HttpResponseVo checkResponseVo(String responseStr) {
        if (MockJsUtils.isJson(responseStr)) {
            return JsonUtils.fromJson(responseStr, HttpResponseVo.class);
        }
        return null;
    }

    private HttpResponseVo createResponseVo(Integer statusCode, String bodyStr) {
        HttpResponseVo responseVo = new HttpResponseVo();
        responseVo.setStatusCode(statusCode);
        responseVo.setBodyStr(StringUtils.defaultString(bodyStr));
        responseVo.setBody(MockJsUtils.getObjectBody(responseVo.getBodyStr()));
        return responseVo;
    }

    protected Pair<String, HttpResponseVo> executePostProcessor(String postProcessor) {
        MockDiagnoseRecorder diagnoseRecorder = MockDiagnoseRecorder.of(MockDiagnoseContext.get());
        diagnoseRecorder.postProcessorStart();
        long startTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder("(async function(){\n");
        sb.append("const bodyStr = await mockStringify(");
        sb.append(MockJsUtils.getJsExpression(postProcessor));
        sb.append(");\n");
        sb.append("const responseStr = mockStringify(response);");
        sb.append("return JSON.stringify({bodyStr, responseStr});");
        sb.append("})()");
        try {
            Object mockRes = scriptEngineProvider.eval(sb.toString());
            String responseBody = StringUtils.EMPTY;
            String responseStr = null;
            if (mockRes instanceof SimpleResult) {
                SimpleResult<?> simpleResult = (SimpleResult<?>) mockRes;
                responseBody = JsonUtils.toJson(mockRes);
                if (!simpleResult.isSuccess()) {
                    diagnoseRecorder.postProcessorError(System.currentTimeMillis() - startTime,
                            getSimpleResultMessage(simpleResult));
                    return Pair.of(responseBody, null);
                }
            } else {
                Map<String, String> resultMap = toResultMap(mockRes);
                if (resultMap != null) {
                    responseBody = resultMap.get("bodyStr");
                    responseStr = resultMap.get("responseStr");
                }
            }
            HttpResponseVo processedResponse = checkResponseVo(responseStr);
            diagnoseRecorder.postProcessorReturn(System.currentTimeMillis() - startTime);
            return Pair.of(responseBody, processedResponse);
        } catch (RuntimeException e) {
            diagnoseRecorder.postProcessorError(System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    private String getSimpleResultMessage(SimpleResult<?> simpleResult) {
        Object resultData = simpleResult.getResultData();
        return StringUtils.defaultIfBlank(simpleResult.getMessage(),
                resultData == null ? null : String.valueOf(resultData));
    }

    private Map<String, String> toResultMap(Object mockRes) {
        if (mockRes instanceof String) {
            return JsonUtils.fromJson((String) mockRes, Map.class);
        }
        if (mockRes instanceof Map) {
            return JsonUtils.fromJson(JsonUtils.toJson(mockRes), Map.class);
        }
        return null;
    }

    @Override
    public Object processSse(MockRequest mockRequest, MockData mockData, Object sseItem) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, mockData);
        if (StringUtils.isNotBlank(postProcessor)) {
            String sseBodyStr = sseItem instanceof String ? (String) sseItem : JsonUtils.toJson(sseItem);
            HttpResponseVo responseVo = createResponseVo(mockData == null ? null : mockData.getStatusCode(), sseBodyStr);
            responseVo.setBody(sseItem);
            MockJsUtils.setCurrentResponseVo(responseVo);
            try {
                Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
                Object resultItem = sseItem;
                if (resultPair.getKey() != null) {
                    String processedBodyStr = resultPair.getKey();
                    if (StringUtils.isNotBlank(processedBodyStr)) {
                        if (sseItem instanceof Map || MockJsUtils.isJson(processedBodyStr)) {
                            resultItem = JsonUtils.fromJson(processedBodyStr, Map.class); // Assuming Map for structured events
                            if (resultItem == null) {
                                resultItem = processedBodyStr; // Fallback
                            }
                        } else {
                            resultItem = processedBodyStr;
                        }
                    }
                }
                return resultItem;
            } finally {
                MockJsUtils.removeCurrentResponseVo();
            }
        }
        return sseItem;
    }
}
