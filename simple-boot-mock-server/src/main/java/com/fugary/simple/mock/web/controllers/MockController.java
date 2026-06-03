package com.fugary.simple.mock.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.push.MockPostScriptProcessor;
import com.fugary.simple.mock.push.MockPushProcessor;
import com.fugary.simple.mock.push.MockSsePushProcessor;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockUserService;
import com.fugary.simple.mock.utils.*;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.*;

/**
 * Created on 2020/5/3 20:23 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping(MockConstants.MOCK_PREFIX)
public class MockController {

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockUserService mockUserService;

    @Autowired
    private MockProjectService mockProjectService;

    @Autowired
    private GenericObjectPool<ScriptEngine> scriptEnginePool;

    @Autowired
    @Qualifier("eventStreamThreadPool")
    private ExecutorService eventStreamThreadPool;

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

    @Autowired
    private MockPushProcessor mockPushProcessor;

    @Autowired
    private MockSsePushProcessor mockSsePushProcessor;

    @Autowired
    private MockPostScriptProcessor mockPostScriptProcessor;

    @RequestMapping("/**")
    public Object doMock(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = request.getHeader(MockConstants.MOCK_REQUEST_ID_HEADER);
        String dataId = request.getHeader(MockConstants.MOCK_DATA_ID_HEADER);
        MockDiagnoseVo diagnose = MockDiagnoseContext.get();
        if (diagnose == null) {
            diagnose = new MockDiagnoseVo();
        }
        Triple<MockGroup, MockRequest, MockData> dataPair = mockGroupService.matchMockData(request,
                NumberUtils.toInt(requestId), NumberUtils.toInt(dataId), mockGroup -> {
                    if (StringUtils.isBlank(requestId) && StringUtils.isBlank(dataId)) {
                        MockUser mockUser = mockUserService.loadValidUser(mockGroup.getUserName());
                        return mockUser != null && mockProjectService.checkProjectValid(mockGroup.getUserName(),
                                mockGroup.getProjectId(), mockGroup.getProjectCode());
                    }
                    return true;
                }, diagnose);
        MockData data = dataPair.getRight();
        MockRequest mockRequest = dataPair.getMiddle();
        MockGroup mockGroup = dataPair.getLeft();
        long start = System.currentTimeMillis();
        ResponseEntity<?> responseEntity = ResponseEntity.notFound().build();
        Pair<Integer, String> delayInfo = mockGroupService.calcDelayInfo(mockGroup, mockRequest, data);
        Integer delayTime = delayInfo == null ? null : delayInfo.getLeft();
        if (delayTime != null && delayTime > 0) {
            response.setHeader(MockConstants.MOCK_DELAY_TIME_HEADER, String.valueOf(delayTime));
        }
        String contentType = SimpleMockUtils.calcContentType(mockGroup, mockRequest, data);
        String proxyTargetUrl = null;
        Pair<String, String> proxyUrlInfo = null;
        if (data != null) {
            HttpHeaders httpHeaders = SimpleMockUtils.calcHeaders(data.getHeaders());
            HttpStatus httpStatus = HttpStatus.resolve(data.getStatusCode());
            if (httpStatus != null && httpStatus.is3xxRedirection()) { // 重定向
                delayTime(start, delayInfo, diagnose, mockGroup, mockRequest, data);
                finishMockDiagnose(diagnose, CODE_MOCK_REDIRECT, data, contentType);
                if (SimpleMockUtils.isMockPreview(request)) {
                    return ResponseEntity.status(HttpStatus.OK).header(MockConstants.MOCK_DATA_REDIRECT_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(JsonUtils.toJson(SimpleResultUtils.createError(MockErrorConstants.CODE_0, "Test redirect, please copy redirect URL to browser to visit")
                                    .addInfo("redirectUrl", data.getResponseBody())));
                }
                return ResponseEntity.status(data.getStatusCode()).headers(httpHeaders)
                        .header(HttpHeaders.LOCATION, data.getResponseBody()).body(null);
            }
            if (StringUtils.contains(contentType, MediaType.TEXT_EVENT_STREAM_VALUE)) {
                delayTime(start, delayInfo, diagnose, mockGroup, mockRequest, data);
                httpHeaders.forEach((k, v) -> v.forEach(val -> response.setHeader(k, val)));
                response.setHeader(MockConstants.MOCK_DATA_USER_HEADER, mockGroup.getUserName());
                response.setHeader(MockConstants.MOCK_DATA_ID_HEADER, String.valueOf(data.getId()));
                MockJsUtils.invalidateCurrentAndPrepareScriptEngine(scriptEnginePool);
                finishMockDiagnose(diagnose, CODE_MOCK_SSE_RETURN, data, contentType);
                return MockEventStreamUtils.processSseRequest(request, response, data, eventStreamThreadPool,
                        mockPostScriptProcessor, mockRequest);
            }
            response.setHeader(MockConstants.MOCK_DATA_ID_HEADER, String.valueOf(data.getId()));
            Pair<String, Object> bodyPair = SimpleMockUtils.getMockResponseBody(data, contentType);
            contentType = bodyPair.getKey();
            responseEntity = ResponseEntity.status(data.getStatusCode())
                    .headers(httpHeaders)
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(bodyPair.getValue());
            SimpleLogUtils.addResponseData(data.getResponseBody());
        } else if (mockGroup != null
                && (proxyUrlInfo = SimpleMockUtils.calcValidProxyUrlInfo(mockGroup, mockRequest)) != null) {
            // 所有request没有匹配上,但是有proxy地址
            String proxyUrl = proxyUrlInfo.getLeft();
            String loopCountStr = request.getHeader(MockConstants.SIMPLE_BOOT_MOCK_HEADER);
            if (NumberUtils.toInt(loopCountStr, 0) > 2) {
                finishProxyDiagnose(diagnose, RESULT_TYPE_ERROR, CODE_PROXY_LOOP_DETECTED, proxyUrl, proxyUrlInfo);
                return ResponseEntity.status(HttpStatus.LOOP_DETECTED).contentType(MediaType.APPLICATION_JSON)
                        .body(JsonUtils.toJson(SimpleResultUtils.createError("Proxy Loop Detected")));
            }
            MockParamsVo mockParams = SimpleMockUtils.toMockParams(mockGroup, mockRequest, request);
            proxyTargetUrl = calcProxyTargetUrl(proxyUrl, mockParams, request);
            // 检测是否是 SSE 请求
            String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);
            if (StringUtils.contains(acceptHeader, MediaType.TEXT_EVENT_STREAM_VALUE)
                    || StringUtils.contains(contentType, MediaType.TEXT_EVENT_STREAM_VALUE)) {
                // SSE 代理请求
                delayTime(start, delayInfo, diagnose, mockGroup, mockRequest, data);
                response.setHeader(MockConstants.MOCK_PROXY_URL_HEADER, proxyTargetUrl);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                MockJsUtils.invalidateCurrentAndPrepareScriptEngine(scriptEnginePool);
                diagnose.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
                finishProxyDiagnose(diagnose, RESULT_TYPE_PROXY, CODE_PROXY_SSE_RETURN, proxyTargetUrl, proxyUrlInfo);
                return mockSsePushProcessor.processSseProxy(
                        mockParams, mockRequest, null);
            }
            // 普通代理请求
            responseEntity = mockPushProcessor.doPush(mockParams);
            responseEntity = mockPostScriptProcessor.process(mockRequest, responseEntity);
            response.setHeader(MockConstants.MOCK_PROXY_URL_HEADER, proxyTargetUrl);
            diagnose.setContentType(calcResponseContentType(responseEntity, contentType));
            SimpleLogUtils.addResponseData(responseEntity);
        }
        if (mockGroup != null) {
            response.setHeader(MockConstants.MOCK_DATA_USER_HEADER, mockGroup.getUserName());
            if (Boolean.TRUE.equals(mockGroup.getDisableMock())
                    || (mockRequest != null && Boolean.TRUE.equals(mockRequest.getDisableMock()))) {
                response.setHeader(MockConstants.MOCK_DATA_PAUSED_HEADER, "true");
            }
        }
        delayTime(start, delayInfo, diagnose, mockGroup, mockRequest, data);
        if (data != null) {
            finishMockDiagnose(diagnose, CODE_MOCK_RETURN, data, contentType);
        } else if (proxyUrlInfo != null) {
            finishProxyDiagnose(diagnose, RESULT_TYPE_PROXY, CODE_PROXY_RETURN,
                    StringUtils.defaultIfBlank(proxyTargetUrl, proxyUrlInfo.getLeft()), proxyUrlInfo);
        } else {
            diagnose.finish(RESULT_TYPE_NONE, CODE_NO_MOCK_OR_PROXY);
        }
        return responseEntity;
    }

    private void delayTime(long start, Pair<Integer, String> delayInfo, MockDiagnoseVo diagnose,
            MockGroup mockGroup, MockRequest mockRequest, MockData data) {
        long actualDelayMs = mockGroupService.delayTime(start, delayInfo == null ? null : delayInfo.getLeft());
        MockDiagnoseRecorder.of(diagnose).delayResolved(delayInfo, actualDelayMs, mockGroup, mockRequest, data);
    }

    private void finishMockDiagnose(MockDiagnoseVo diagnose, String code, MockData data, String contentType) {
        diagnose.setContentType(contentType);
        diagnose.setDataInfo(data, contentType);
        diagnose.finish(RESULT_TYPE_MOCK, code, KEY_DATA, diagnose.getData());
    }

    private void finishProxyDiagnose(MockDiagnoseVo diagnose, String resultType, String code, String proxyUrl,
            Pair<String, String> proxyUrlInfo) {
        String proxySource = proxyUrlInfo.getRight();
        diagnose.setProxyUrl(proxyUrl);
        diagnose.setProxySource(proxySource);
        diagnose.finish(resultType, code, KEY_PROXY_URL, proxyUrl, KEY_PROXY_SOURCE, proxySource);
    }

    private String calcResponseContentType(ResponseEntity<?> responseEntity, String defaultContentType) {
        MediaType mediaType = responseEntity.getHeaders().getContentType();
        return mediaType == null ? defaultContentType : mediaType.toString();
    }

    private String calcProxyTargetUrl(String proxyUrl, MockParamsVo mockParams, HttpServletRequest request) {
        String targetUrl = StringUtils.removeEnd(proxyUrl, "/")
                + StringUtils.prependIfMissing(StringUtils.defaultString(mockParams.getRequestPath()), "/");
        String queryString = request.getQueryString();
        return StringUtils.isBlank(queryString) ? targetUrl : targetUrl + "?" + queryString;
    }

    /**
     * 表达式测试
     *
     * @param request
     * @return
     */
    @RequestMapping("/checkMatchPattern")
    public SimpleResult<Object> checkMatchPattern(HttpServletRequest request) {
        try {
            HttpRequestVo requestVo = calcRequestVo(request);
            MockJsUtils.setCurrentRequestVo(requestVo);
            String matchPattern = request.getParameter(MockConstants.MOCK_DATA_MATCH_PATTERN_HEADER);
            if (StringUtils.isBlank(matchPattern)) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400);
            }
            matchPattern = URLDecoder.decode(matchPattern, StandardCharsets.UTF_8);
            Object result = scriptEngineProvider.eval(matchPattern);
            if (result instanceof SimpleResult) {
                return (SimpleResult) result;
            }
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_0, result);
        } catch (Exception e) {
            log.error("checkMatchPattern error", e);
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, e.getMessage());
        } finally {
            MockJsUtils.removeCurrentRequestVo();
        }
    }

    private HttpRequestVo calcRequestVo(HttpServletRequest request) {
        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);
        String pathParamsHeader = request.getParameter(MockConstants.MOCK_DATA_PATH_PARAMS_HEADER);
        if (StringUtils.isNotBlank(pathParamsHeader)) {
            pathParamsHeader = URLDecoder.decode(pathParamsHeader, StandardCharsets.UTF_8);
            try {
                List<NameValue> pathParams = JsonUtils.getMapper().readValue(pathParamsHeader, new TypeReference<>() {
                });
                if (pathParams != null) {
                    requestVo.setPathParameters(pathParams.stream().collect(Collectors
                            .toMap(NameValue::getName, NameValue::getValue)));
                }
            } catch (JsonProcessingException e) {
                log.error("解析路径参数错误", e);
            }
        }
        return requestVo;
    }
}
