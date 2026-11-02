package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiProvider;
import com.fugary.simple.mock.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Shared bounded transport; provider credentials and response bodies never enter errors. */
public abstract class AbstractAiChatProvider implements AiChatProvider {
    private final RestTemplate restTemplate;

    protected AbstractAiChatProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        restTemplate = new RestTemplate(factory);
    }

    @Override
    public AiChatResponse chat(AiConfig config, AiChatRequest request) {
        return chat(config, request, null);
    }

    protected JsonNode post(String url, HttpHeaders headers, Object body) {
        return exchange(url, HttpMethod.POST, headers, body);
    }

    protected JsonNode get(String url, HttpHeaders headers) {
        return exchange(url, HttpMethod.GET, headers, null);
    }

    private JsonNode exchange(String url, HttpMethod method, HttpHeaders headers, Object body) {
        try {
            String response = restTemplate.exchange(URI.create(url), method, new HttpEntity<>(body, headers), String.class).getBody();
            if (StringUtils.isBlank(response)) {
                throw new AiException(500, "simple.ai.invalidResponse");
            }
            JsonNode parsed = JsonUtils.getMapper().readTree(response);
            if (parsed == null || !parsed.isObject()) {
                throw new AiException(500, "simple.ai.invalidResponse");
            }
            return parsed;
        } catch (RestClientResponseException e) {
            throw new AiException(500, "simple.ai.httpError", e.getRawStatusCode());
        } catch (ResourceAccessException e) {
            throw new AiException(500, e.getRootCause() instanceof SocketTimeoutException
                ? "simple.ai.timeout" : "simple.ai.requestFailed");
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException(500, "simple.ai.invalidResponse");
        }
    }

    protected JsonNode post(String url, HttpHeaders headers, Map<String, Object> body, AiStreamSession stream) {
        if (stream == null) return post(url, headers, body);
        stream.check();
        Map<String, Object> streamingBody = new HashMap<>(body);
        if (getProvider() == AiProvider.GEMINI) {
            url = url.replace(":generateContent", ":streamGenerateContent") + "?alt=sse";
        } else {
            streamingBody.put("stream", true);
            if (getProvider() == AiProvider.OPENAI) streamingBody.put("stream_options", Map.of("include_usage", true));
        }
        HttpPost post = new HttpPost(url);
        headers.forEach((key, values) -> values.forEach(value -> post.addHeader(key, value)));
        post.setHeader("Accept", "text/event-stream");
        post.setConfig(RequestConfig.custom()
                .setConnectTimeout(60000).setSocketTimeout(60000)
                .setConnectionRequestTimeout(60000).build());
        stream.bind(post::abort);
        try (CloseableHttpClient client = HttpClients.custom()
                .useSystemProperties().disableAutomaticRetries().disableRedirectHandling().build()) {
            post.setEntity(new StringEntity(JsonUtils.getMapper().writeValueAsString(streamingBody),
                    StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 300) {
                    throw new AiException(500, "simple.ai.httpError", status);
                }
                if (response.getEntity() == null || response.getEntity().getContentType() == null
                        || !response.getEntity().getContentType().getValue().toLowerCase(Locale.ROOT).contains("text/event-stream")) {
                    throw new AiException(500, "simple.ai.invalidResponse");
                }
                AiStreamAccumulator accumulator = new AiStreamAccumulator(getProvider(), JsonUtils.getMapper(), stream);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    StringBuilder data = new StringBuilder();
                    String line;
                    int received = 0;
                    while ((line = reader.readLine()) != null) {
                        stream.check();
                        received += line.length();
                        if (received > 8 * 1024 * 1024) throw new IOException("AI 流式响应超出大小限制");
                        if (line.isEmpty()) {
                            if (data.length() > 0) {
                                accumulator.accept(data.toString());
                                data.setLength(0);
                                if (accumulator.isTerminal()) break;
                            }
                        } else if (line.startsWith("data:")) {
                            if (data.length() > 0) data.append('\n');
                            String value = line.substring(5);
                            data.append(value.startsWith(" ") ? value.substring(1) : value);
                        }
                    }
                    // 不分发缺少空行结束的残缺事件。
                    return accumulator.result();
                }
            }
        } catch (IOException e) {
            stream.check();
            throw new AiException(500, e instanceof SocketTimeoutException ? "simple.ai.timeout" : "simple.ai.invalidResponse");
        } finally {
            stream.unbind();
        }
    }

    protected String url(String base, String path) {
        return StringUtils.stripEnd(base, "/") + path;
    }

    protected HttpHeaders headers(String name, String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(value)) {
            headers.set(name, value);
        }
        return headers;
    }

    protected Integer tokenCount(JsonNode usage, String field) {
        JsonNode value = usage.path(field);
        return value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= 0 ? value.intValue() : null;
    }

    protected AiChatResponse response(String content) {
        if (StringUtils.isBlank(content)) {
            throw new AiException(500, "simple.ai.invalidResponse");
        }
        AiChatResponse response = new AiChatResponse();
        response.setContent(content);
        return response;
    }

    protected List<String> modelIds(JsonNode data) {
        if (!data.isArray()) {
            throw new AiException(500, "simple.ai.invalidResponse");
        }
        Set<String> models = new TreeSet<>();
        for (JsonNode item : data) {
            String id = item.path("id").asText();
            if (StringUtils.isNotBlank(id)) {
                models.add(id);
            }
            if (models.size() >= 1000) {
                break;
            }
        }
        return new ArrayList<>(models);
    }

}
