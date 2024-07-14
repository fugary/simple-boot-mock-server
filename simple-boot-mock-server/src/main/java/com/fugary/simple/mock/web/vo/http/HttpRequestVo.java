package com.fugary.simple.mock.web.vo.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Create date 2024/7/2<br>
 *
 * @author gary.fu
 */
@Slf4j
@Data
public class HttpRequestVo {

    private String url;
    private String method;
    private String contentType;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();
    private Map<String, String> pathParameters = new HashMap<>();
    private Object body;
    private String headersStr;
    private String parametersStr;
    private String bodyStr;
    private String path;
    private String host;
    private String protocol;

    public void setUrl(String url) {
        this.url = url;
        try {
            URIBuilder builder = new URIBuilder(url);
            setPath(builder.getPath());
            setHost(builder.getHost());
            setProtocol(builder.getScheme());
        } catch (URISyntaxException e) {
            log.error("url格式错误", e);
        }
    }

    public Map<String, String> getParams() {
        Map<String, String> paramsMap = new HashMap<>(getParameters());
        paramsMap.putAll(getPathParameters());
        return paramsMap;
    }

    public Map<String, String> getQuery() {
        return getParameters();
    }

    public Map<String, String> getHeader() {
        return headers;
    }

    public String getType() {
        return contentType;
    }

    public String getQueryString(){
        return getParametersStr();
    }
}
