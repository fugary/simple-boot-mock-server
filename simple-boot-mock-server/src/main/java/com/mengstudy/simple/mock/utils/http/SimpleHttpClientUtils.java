package com.mengstudy.simple.mock.utils.http;

import com.mengstudy.simple.mock.utils.JsonUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HttpClient工具 <br>
 * Created on 2020/5/1 14:56 .<br>
 *
 * @author gary.fu
 */
public class SimpleHttpClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHttpClientUtils.class);

    private SimpleHttpClientUtils() {
    }

    public static final PoolingHttpClientConnectionManager CLIENT_CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();

    public static final RequestConfig REQUEST_CONFIG;

    public static final int POOL_SIZE = 200; // HttpClient连接池大小

    public static final int MAX_PER_ROUTE = 100;

    public static final int CONNECT_TIMEOUT = 15000; // 连接超时

    public static final int SOCKET_TIMEOUT = 60000; // 数据获取超时

    static {
        CLIENT_CONNECTION_MANAGER.setMaxTotal(POOL_SIZE);
        CLIENT_CONNECTION_MANAGER.setDefaultMaxPerRoute(MAX_PER_ROUTE);
        REQUEST_CONFIG = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setConnectionRequestTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
    }

    /**
     * 获取httpclient
     *
     * @return
     */
    public static CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().setDefaultRequestConfig(REQUEST_CONFIG).setConnectionManager(CLIENT_CONNECTION_MANAGER).build();
    }

    public static RequestConfig getRequestConfig(int connectTimeout, int soTimeout) {
        return RequestConfig.custom().setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectTimeout).setSocketTimeout(soTimeout).build();
    }

    /**
     * 发送简单get请求
     *
     * @param getUrl
     * @param clazz
     * @param paramMap
     * @param <T>
     * @return
     */
    public static <T> T sendHttpGet(String getUrl, Class<T> clazz, Map<String, String> paramMap){
        List<NameValuePair> params = paramMap.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        return sendHttpGet(getUrl, clazz, (httpClient, httpGet) -> {
            try {
                URI url = new URIBuilder(getUrl).addParameters(params).build();
                ((HttpGet) httpGet).setURI(url);
            } catch (URISyntaxException e) {
                logger.error("处理URL错误", e);
            }
        });
    }

    /**
     * 发送GET请求
     *
     * @param url      请求地址
     * @param clazz    返回类型
     * @param callback 回调，构建请求
     * @param <T>
     * @return
     */
    public static <T> T sendHttpGet(String url, Class<T> clazz, HttpClientRequestCallback callback) {
        HttpClient client = getHttpClient();
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = null;
        try {
            if (callback != null) {
                callback.processRequest(client, httpGet);
            }
            response = client.execute(httpGet);
            return calcResponse(response, clazz);
        } catch (Exception e) {
            logger.error("执行GET请求错误", e);
        } finally {
            httpGet.releaseConnection();
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 发送post请求
     *
     * @param url      请求地址
     * @param clazz    返回类型
     * @param callback 回调，构建请求
     * @param <T>
     * @return
     */
    public static <T> T sendHttpPost(String url, Class<T> clazz, HttpClientRequestCallback callback) {
        HttpClient client = getHttpClient();
        HttpPost httpPost = new HttpPost(url);
        HttpResponse response = null;
        try {
            if (callback != null) {
                callback.processRequest(client, httpPost);
            }
            response = client.execute(httpPost);
            return calcResponse(response, clazz);
        } catch (Exception e) {
            logger.error("执行POST请求错误", e);
        } finally {
            httpPost.releaseConnection();
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 返回类型定制
     *
     * @param response httpclient返回值
     * @param clazz    需要解析成的返回类型，如果不需要解析，可以用<code>HttpResponse</code>或者<code>HttpEntity</code>，自己解析内容
     * @param <T>
     * @return
     * @throws IOException
     * @throws JAXBException
     */
    public static <T> T calcResponse(HttpResponse response, Class<T> clazz) throws IOException, JAXBException {
        HttpEntity httpEntity = response.getEntity();
        if (HttpResponse.class.isAssignableFrom(clazz)) {
            return (T) response;
        } else if (HttpEntity.class.isAssignableFrom(clazz)) {
            return (T) httpEntity;
        } else if (String.class.isAssignableFrom(clazz)) {
            String resultStr = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
            logger.debug("RES: {}", resultStr);
            return (T) resultStr;
        } else {
            String resultStr = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
            logger.debug("RES: {}", resultStr);
            return JsonUtils.fromJson(resultStr, clazz);
        }
    }

}
