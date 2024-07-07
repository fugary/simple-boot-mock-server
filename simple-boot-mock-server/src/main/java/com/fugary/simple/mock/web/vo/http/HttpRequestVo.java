package com.fugary.simple.mock.web.vo.http;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Create date 2024/7/2<br>
 *
 * @author gary.fu
 */
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

}
