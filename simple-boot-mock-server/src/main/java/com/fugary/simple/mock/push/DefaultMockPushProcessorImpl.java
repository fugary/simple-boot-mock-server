package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.vo.NameValue;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Create date 2024/7/17<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class DefaultMockPushProcessorImpl implements MockPushProcessor {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public ResponseEntity<byte[]> doPush(MockParamsVo mockParams) {
        String requestUrl = getRequestUrl(mockParams.getTargetUrl(), mockParams);
        HttpEntity<?> entity = new HttpEntity<>(mockParams.getRequestBody(), getHeaders(mockParams));
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(requestUrl,
                    Optional.ofNullable(HttpMethod.resolve(mockParams.getMethod())).orElse(HttpMethod.GET),
                    entity, byte[].class);
            responseEntity = processRedirect(responseEntity, mockParams, entity);
            return SimpleMockUtils.removeCorsHeaders(responseEntity);
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            log.error("获取数据错误", e);
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<byte[]> processRedirect(ResponseEntity<byte[]> responseEntity,
                                                   MockParamsVo mockParams,
                                                   HttpEntity<?> entity) {
        HttpStatus httpStatus = responseEntity.getStatusCode();
        if (httpStatus.is3xxRedirection()) {
            URI location = responseEntity.getHeaders().getLocation();
            if (location != null) {
                URI targetUri = UriComponentsBuilder.fromUri(location)
                        .queryParams(getQueryParams(mockParams))
                        .build(true).toUri();
                responseEntity = restTemplate.exchange(targetUri,
                        Optional.ofNullable(HttpMethod.resolve(mockParams.getMethod())).orElse(HttpMethod.GET),
                        entity, byte[].class);
            }
        }
        return responseEntity;
    }

    /**
     * 计算参数
     *
     * @param mockRequest
     * @return
     */
    protected MockParamsVo loadMockParams(MockRequest mockRequest) {
        MockParamsVo mockParamsVo = new MockParamsVo();
        if (StringUtils.isNotBlank(mockRequest.getMockParams())) {
            mockParamsVo = JsonUtils.fromJson(mockRequest.getMockParams(), MockParamsVo.class);
        }
        mockParamsVo.setRequestPath(mockRequest.getRequestPath());
        mockParamsVo.setMethod(mockRequest.getMethod());
        return mockParamsVo;
    }

    /**
     * 计算请求url地址
     *
     * @param baseUrl
     * @param mockParams
     * @return
     */
    protected String getRequestUrl(String baseUrl, MockParamsVo mockParams) {
        String requestUrl = mockParams.getRequestPath();
        requestUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path(requestUrl)
                .queryParams(getQueryParams(mockParams))
                .build(true).toUriString();
        requestUrl = requestUrl.replaceAll(":([\\w-]+)", "{$1}"); // spring 支持的ant path不支持:var格式，只支持{var}格式
        for (NameValue nv : mockParams.getPathParams()) {
            requestUrl = requestUrl.replace("{" + nv.getName() + "}", nv.getValue());
        }
        return requestUrl;
    }

    /**
     * 获取头信息
     *
     * @param paramsVo
     * @return
     */
    protected HttpHeaders getHeaders(MockParamsVo paramsVo) {
        HttpHeaders headers = new HttpHeaders();
        paramsVo.getHeaderParams().forEach(nv -> headers.addIfAbsent(nv.getName(), nv.getValue()));
        return headers;
    }

    /**
     * 获取参数
     *
     * @param paramsVo
     * @return
     */
    protected MultiValueMap<String, String> getQueryParams(MockParamsVo paramsVo) {
        return new MultiValueMapAdapter<>(paramsVo.getRequestParams().stream()
                .collect(Collectors.toMap(NameValue::getName, nv -> List.of(nv.getValue()))));
    }
}
