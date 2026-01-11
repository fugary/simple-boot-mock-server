package com.fugary.simple.mock.push;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.script.ScriptEngineProvider;
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
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(mockData.getStatusCode());
            responseVo.setBodyStr(responseBody);
            responseVo.setBody(MockJsUtils.getObjectBody(responseVo.getBodyStr()));
            MockJsUtils.setCurrentResponseVo(responseVo);
            Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
            overrideResponse(mockData, resultPair.getValue());
            responseBody = resultPair.getKey();
        }
        return responseBody;
    }

    @Override
    public ResponseEntity<?> process(MockRequest mockRequest, ResponseEntity<?> responseEntity) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, null);
        if (StringUtils.isNotBlank(postProcessor)) {
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(responseEntity.getStatusCode().value());
            Object body = responseEntity.getBody();
            String responseBody = StringUtils.EMPTY;
            if (body instanceof byte[]) {
                responseBody = new String((byte[]) body, StandardCharsets.UTF_8);
            } else if (body instanceof String) {
                responseBody = (String) body;
            }
            responseVo.setBodyStr(responseBody);
            responseVo.setBody(MockJsUtils.getObjectBody(responseVo.getBodyStr()));
            MockJsUtils.setCurrentResponseVo(responseVo);
            Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
            responseBody = resultPair.getKey();
            byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            responseEntity = overrideResponse(responseEntity, bodyBytes, resultPair.getValue());
        }
        return responseEntity;
    }

    protected void overrideResponse(MockData mockData, HttpResponseVo httpResponse) {
        if (httpResponse != null) {
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

    protected Pair<String, HttpResponseVo> executePostProcessor(String postProcessor) {
        StringBuilder sb = new StringBuilder("(async function(){\n");
        sb.append("const bodyStr = await mockStringify(");
        sb.append(MockJsUtils.getJsExpression(postProcessor));
        sb.append(");\n");
        sb.append("const responseStr = mockStringify(response);");
        sb.append("return {bodyStr, responseStr};");
        sb.append("})()");
        Object mockRes = scriptEngineProvider.eval(sb.toString());
        String responseBody = StringUtils.EMPTY;
        String responseStr = null;
        if (mockRes instanceof SimpleResult) {
            responseBody = JsonUtils.toJson(mockRes);
        } else if (mockRes instanceof Map) {
            String json = JsonUtils.toJson(mockRes);
            mockRes = JsonUtils.fromJson(json, Map.class);
            responseBody = ((Map<String, String>) mockRes).get("bodyStr");
            responseStr = ((Map<String, String>) mockRes).get("responseStr");
        }
        return Pair.of(responseBody, checkResponseVo(responseStr));
    }

    @Override
    public Object processSse(MockRequest mockRequest, MockData mockData, Object sseItem) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, mockData);
        if (StringUtils.isNotBlank(postProcessor)) {
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setBody(sseItem);
            if (sseItem instanceof String) {
                responseVo.setBodyStr((String) sseItem);
            } else {
                responseVo.setBodyStr(JsonUtils.toJson(sseItem));
            }
            MockJsUtils.setCurrentResponseVo(responseVo);
            try {
                Pair<String, HttpResponseVo> resultPair = executePostProcessor(postProcessor);
                Object resultItem = sseItem;
                if (resultPair.getKey() != null) {
                    String bodyStr = resultPair.getKey();
                    if (StringUtils.isNotBlank(bodyStr)) {
                        if (sseItem instanceof Map || MockJsUtils.isJson(bodyStr)) {
                            resultItem = JsonUtils.fromJson(bodyStr, Map.class); // Assuming Map for structured events
                            if (resultItem == null) {
                                resultItem = bodyStr; // Fallback
                            }
                        } else {
                            resultItem = bodyStr;
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
