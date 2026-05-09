package com.fugary.simple.mock.utils.servlet;

import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpRequestUtilsTest {

    @Test
    void parseRequestVoShouldExposeRequestMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setScheme("https");
        request.setServerName("mock.local");
        request.setServerPort(9443);
        request.setContextPath("/app");
        request.setServletPath("/mock/demo/users");
        request.setPathInfo("/1");
        request.setRequestURI("/app/mock/demo/users/1");
        request.setQueryString("type=vip");
        request.addParameter("type", "vip");
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader(HttpHeaders.USER_AGENT, "JUnit-Agent");
        request.addHeader(HttpHeaders.ORIGIN, "https://example.com");
        request.addHeader(HttpHeaders.REFERER, "https://example.com/form");
        request.setCookies(new Cookie("sid", "abc123"));
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent("{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8));

        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);

        assertEquals("203.0.113.9", requestVo.getIp());
        assertEquals("JUnit-Agent", requestVo.getUserAgent());
        assertEquals("type=vip", requestVo.getQueryString());
        assertEquals("vip", requestVo.getParameters().get("type"));
        assertEquals("abc123", requestVo.getCookies().get("sid"));
        assertEquals("test", ((Map<?, ?>) requestVo.getBody()).get("name"));
    }
}
