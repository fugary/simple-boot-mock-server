package com.fugary.simple.mock.web.controllers;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private RestTemplate restTemplate;

    @RequestMapping("/**")
    public ResponseEntity<?> doMock(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String dataId = request.getHeader(MockConstants.MOCK_DATA_ID_HEADER);
        Triple<MockGroup, MockRequest, MockData> dataPair = mockGroupService.matchMockData(request, NumberUtils.toInt(dataId));
        MockData data = dataPair.getRight();
        MockGroup mockGroup = dataPair.getLeft();
        long start = System.currentTimeMillis();
        ResponseEntity<?> responseEntity = ResponseEntity.notFound().build();
        if (data != null) {
            HttpHeaders httpHeaders = SimpleMockUtils.calcHeaders(data.getHeaders());
            if (HttpStatus.MOVED_TEMPORARILY.value() == data.getStatusCode()) { // 重定向
                if (SimpleMockUtils.isMockPreview(request)) {
                    return ResponseEntity.ok("重定向请设为默认响应后复制URL到浏览器访问");
                }
                return ResponseEntity.status(data.getStatusCode()).headers(httpHeaders).header(HttpHeaders.LOCATION, data.getResponseBody()).body(null);
            }
            responseEntity = ResponseEntity.status(data.getStatusCode())
                    .headers(httpHeaders)
                    .header(HttpHeaders.CONTENT_TYPE, data.getContentType())
                    .body(data.getResponseBody());
        } else if (mockGroup != null && SimpleMockUtils.isValidProxyUrl(mockGroup.getProxyUrl())) {
            // 所有request没有匹配上,但是有proxy地址
            responseEntity = proxy(mockGroup.getProxyUrl(), request, response);
        }
        mockGroupService.delayTime(start, mockGroupService.calcDelayTime(dataPair.getLeft(), dataPair.getMiddle(), dataPair.getRight()));
        return responseEntity;
    }

    @RequestMapping("/proxy/**")
    public ResponseEntity<byte[]> proxy(@RequestParam(value = "_url", required = false) String proxyUrl, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (SimpleMockUtils.isValidProxyUrl(proxyUrl)) {
            String pathPrefix = request.getContextPath() + "/mock/\\w+(/.*)";
            String requestUrl = request.getRequestURI();
            Matcher matcher = Pattern.compile(pathPrefix).matcher(requestUrl);
            if (matcher.matches()) {
                requestUrl = matcher.group(1);
            }
            URI targetUri = UriComponentsBuilder.fromUriString(proxyUrl)
                    .path(requestUrl)
                    .query(request.getQueryString())
                    .replaceQueryParam("_url")
                    .build(true).toUri();
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                boolean excludeHeader = SimpleMockUtils.getExcludeHeaders().contains(headerName.toLowerCase());
                if (SimpleMockUtils.isMockPreview(request)) {
                    excludeHeader = SimpleMockUtils.isExcludeHeaders(headerName.toLowerCase());
                }
                if (!excludeHeader && StringUtils.isNotBlank(headerValue)) {
                    headers.add(headerName, headerValue);
                }
            }
            headers.add(MockConstants.SIMPLE_BOOT_MOCK_HEADER, "1");
            InputStreamResource resource = new InputStreamResource(request.getInputStream());
            HttpEntity<?> entity = new HttpEntity<>(resource, headers);
            try {
                ResponseEntity<byte[]> responseEntity = restTemplate.exchange(targetUri, Optional.ofNullable(HttpMethod.resolve(request.getMethod())).orElse(HttpMethod.GET),
                        entity, byte[].class);
                return SimpleMockUtils.removeCorsHeaders(responseEntity);
            } catch (HttpClientErrorException e) {
                return ResponseEntity.status(e.getStatusCode())
                        .headers(e.getResponseHeaders())
                        .body(e.getResponseBodyAsByteArray());
            } catch (Exception e) {
                log.error("获取数据错误", e);
            }
        }
        return ResponseEntity.notFound().build();
    }
}
