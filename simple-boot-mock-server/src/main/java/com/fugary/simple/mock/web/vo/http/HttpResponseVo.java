package com.fugary.simple.mock.web.vo.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Create date 2026/1/8<br>
 *
 * @author gary.fu
 */
@Slf4j
@Data
public class HttpResponseVo {
    private Integer statusCode;
    private Object body;
    private String bodyStr;
    private Map<String, String> headers = new HashMap<>();
}
