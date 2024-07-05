package com.fugary.simple.mock.utils.http;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

/**
 * Created on 2020/5/1 15:03 .<br>
 *
 * @author gary.fu
 */
@FunctionalInterface
public interface HttpClientRequestCallback {

    /**
     * 处理请求对象
     *
     * @param httpClient
     * @param request
     * @return
     */
    void processRequest(HttpClient httpClient, HttpUriRequest request) throws IOException;
}
