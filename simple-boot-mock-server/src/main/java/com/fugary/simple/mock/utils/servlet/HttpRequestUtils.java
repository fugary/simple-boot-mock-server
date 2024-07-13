/**
 *
 */
package com.fugary.simple.mock.utils.servlet;

import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpRequestUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);

	private static final String UNKNOWN_KEY = "unknown";

	/**
	 * 取得访问IP
	 *
	 * @param request
	 * @return
	 */
	public static String getIp(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (StringUtils.isBlank(ip) || UNKNOWN_KEY.equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (StringUtils.isBlank(ip) || UNKNOWN_KEY.equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (StringUtils.isBlank(ip) || UNKNOWN_KEY.equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (StringUtils.isBlank(ip) || UNKNOWN_KEY.equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	public static String getRequestUrl(HttpServletRequest request){
		StringBuffer url = request.getRequestURL();
		String queryString = request.getQueryString();
		if (StringUtils.isNotBlank(queryString)) {
			url.append("?").append(queryString);
		}
		return url.toString();
	}

	public static HttpSession getCurrentSession() {
		HttpServletRequest currentRequest = getCurrentRequest();
		if (currentRequest != null) {
			return currentRequest.getSession();
		}
		return null;
	}

	public static HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return requestAttributes != null ? requestAttributes.getRequest() : null;
	}

	public static HttpServletResponse getCurrentResponse() {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		return requestAttributes != null ? requestAttributes.getResponse() : null;
	}

	public static String getRequestPath(HttpServletRequest request) {
		String url = request.getServletPath();
		if (request.getPathInfo() != null) {
			url += request.getPathInfo();
		}
		return url;
	}

	public static String getClientIp() {
		HttpServletRequest currentRequest = getCurrentRequest();
		String ipAddr = null;
		if (currentRequest != null) {
			ipAddr = getIp(currentRequest);
			try {
				if (StringUtils.isNotBlank(ipAddr) && !ipAddr.contains(",") && InetAddress.getByName(ipAddr).isLoopbackAddress()) {
					ipAddr = InetAddress.getLocalHost().getHostAddress();
				}
			} catch (Exception e) {
				logger.error("获取IP错误", e);
			}
		}
		return ipAddr;
	}

	/**
	 * 处理请求参数
	 */
	public static HttpRequestVo parseRequestVo(HttpServletRequest request) {
		Map<String, String> parameters = new HashMap<>();
		Map<String, String> headers = new HashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			headers.put(name, request.getHeader(name));
		}
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String name = parameterNames.nextElement();
			parameters.put(name, request.getParameter(name));
		}
		HttpRequestVo requestVo = new HttpRequestVo();
		requestVo.setHeaders(headers);
		requestVo.setHeadersStr(JsonUtils.toJson(headers));
		requestVo.setParameters(parameters);
		requestVo.setParametersStr(JsonUtils.toJson(parameters));
		requestVo.setUrl(HttpRequestUtils.getRequestUrl(request));
		requestVo.setMethod(request.getMethod());
		requestVo.setContentType(request.getContentType());
		List<MediaType> mediaTypes = MediaType.parseMediaTypes(request.getContentType());
		if(isCompatibleWith(mediaTypes, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
				MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML)){
            try {
                requestVo.setBodyStr(StreamUtils.copyToString(getBodyResource(request).getInputStream(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.error("parse RequestVo body error", e);
            }
        }
		if(isCompatibleWith(mediaTypes, MediaType.APPLICATION_JSON)){
			requestVo.setBody(HttpRequestUtils.getJsonBody(requestVo.getBodyStr()));
		}
		return requestVo;
	}

	public static Object getJsonBody(String bodyStr) {
		if (MockJsUtils.isJson(bodyStr)) {
			return JsonUtils.fromJson(bodyStr, Map.class);
		}
		return null;
	}

	public static boolean isCompatibleWith(List<MediaType> mediaTypes, MediaType...matchTypes) {
		for (MediaType type : mediaTypes) {
			for (MediaType mediaType : matchTypes) {
				if (mediaType.isCompatibleWith(type)) {
					return true;
				}
			}
		}
		return false;
	}

	public static Resource getBodyResource(HttpServletRequest request) throws IOException {
		Resource bodyResource = new InputStreamResource(request.getInputStream());
		if(request instanceof ContentCachingRequestWrapper){
			ContentCachingRequestWrapper contentCachingRequestWrapper = (ContentCachingRequestWrapper) request;
			if (contentCachingRequestWrapper.getContentAsByteArray().length > 0) {
				bodyResource = new ByteArrayResource(contentCachingRequestWrapper.getContentAsByteArray());
			}
		}
		return bodyResource;
	}
}
