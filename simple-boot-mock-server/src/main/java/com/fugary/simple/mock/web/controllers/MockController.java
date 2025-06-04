package com.fugary.simple.mock.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.push.MockPushProcessor;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockUserService;
import com.fugary.simple.mock.utils.*;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

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
    private ScriptEngineProvider scriptEngineProvider;

    @Autowired
    private MockPushProcessor mockPushProcessor;

    @RequestMapping("/**")
    public ResponseEntity<?> doMock(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = request.getHeader(MockConstants.MOCK_REQUEST_ID_HEADER);
        String dataId = request.getHeader(MockConstants.MOCK_DATA_ID_HEADER);
        Triple<MockGroup, MockRequest, MockData> dataPair = mockGroupService.matchMockData(request, NumberUtils.toInt(requestId), NumberUtils.toInt(dataId), mockGroup -> {
            if (StringUtils.isBlank(requestId) && StringUtils.isBlank(dataId)) {
                MockUser mockUser = mockUserService.loadValidUser(mockGroup.getUserName());
                return mockUser != null && mockProjectService.checkProjectValid(mockGroup.getUserName(), mockGroup.getProjectCode());
            }
            return true;
        });
        MockData data = dataPair.getRight();
        MockRequest mockRequest = dataPair.getMiddle();
        MockGroup mockGroup = dataPair.getLeft();
        long start = System.currentTimeMillis();
        ResponseEntity<?> responseEntity = ResponseEntity.notFound().build();
        if (data != null) {
            HttpHeaders httpHeaders = SimpleMockUtils.calcHeaders(data.getHeaders());
            HttpStatus httpStatus = HttpStatus.resolve(data.getStatusCode());
            if (httpStatus != null && httpStatus.is3xxRedirection()) { // 重定向
                if (SimpleMockUtils.isMockPreview(request)) {
                    return ResponseEntity.status(HttpStatus.OK).header(MockConstants.MOCK_DATA_REDIRECT_HEADER, "1")
                            .body("测试重定向请复制URL到浏览器访问，跳转地址：" + data.getResponseBody());
                }
                return ResponseEntity.status(data.getStatusCode()).headers(httpHeaders).header(HttpHeaders.LOCATION, data.getResponseBody()).body(null);
            }
            response.setHeader(MockConstants.MOCK_DATA_ID_HEADER, String.valueOf(data.getId()));
            response.setHeader(MockConstants.MOCK_DATA_USER_HEADER, mockGroup.getUserName());
            responseEntity = ResponseEntity.status(data.getStatusCode())
                    .headers(httpHeaders)
                    .header(HttpHeaders.CONTENT_TYPE, SimpleMockUtils.getContentType(data.getContentType(), data.getDefaultCharset()))
                    .body(data.getResponseBody());
            SimpleLogUtils.addResponseData(data.getResponseBody());
        } else if (mockGroup != null && SimpleMockUtils.isValidProxyUrl(SimpleMockUtils.calcProxyUrl(mockGroup, mockRequest))) {
            // 所有request没有匹配上,但是有proxy地址
            responseEntity = mockPushProcessor.doPush(SimpleMockUtils.toMockParams(mockGroup, mockRequest, request));
            SimpleLogUtils.addResponseData(responseEntity);
        }
        mockGroupService.delayTime(start, mockGroupService.calcDelayTime(dataPair.getLeft(), dataPair.getMiddle(), dataPair.getRight()));
        return responseEntity;
    }

    /**
     * 表达式测试
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
        } catch (Exception e){
            log.error("checkMatchPattern error", e);
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, e.getMessage());
        }finally {
            MockJsUtils.removeCurrentRequestVo();
        }
    }

    private HttpRequestVo calcRequestVo(HttpServletRequest request) {
        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);
        String pathParamsHeader = request.getParameter(MockConstants.MOCK_DATA_PATH_PARAMS_HEADER);
        if (StringUtils.isNotBlank(pathParamsHeader)) {
            pathParamsHeader = URLDecoder.decode(pathParamsHeader, StandardCharsets.UTF_8);
            try {
                List<NameValue> pathParams = JsonUtils.getMapper().readValue(pathParamsHeader, new TypeReference<>() {});
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
