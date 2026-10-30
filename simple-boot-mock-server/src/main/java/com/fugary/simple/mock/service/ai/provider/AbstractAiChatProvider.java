package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import java.net.SocketTimeoutException;
import java.net.URI;

/** Shared bounded transport; provider credentials and response bodies never enter errors. */
public abstract class AbstractAiChatProvider implements AiChatProvider {
    private final RestTemplate restTemplate;

    protected AbstractAiChatProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        restTemplate = new RestTemplate(factory);
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
