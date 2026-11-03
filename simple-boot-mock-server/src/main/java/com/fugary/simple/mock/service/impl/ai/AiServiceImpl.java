package com.fugary.simple.mock.service.impl.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.service.ai.AiConfigService;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiLogService;
import com.fugary.simple.mock.service.ai.AiProvider;
import com.fugary.simple.mock.service.ai.AiService;
import com.fugary.simple.mock.service.ai.provider.AiCallTrace;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/** Routes a single request and persists one success or failure record without caching. */
@Service
public class AiServiceImpl implements AiService {
    private final AiConfigService configs;
    private final AiLogService logs;
    private final Map<AiProvider, AiChatProvider> providers = new EnumMap<>(AiProvider.class);

    public AiServiceImpl(AiConfigService configs, AiLogService logs, List<AiChatProvider> providers) {
        this.configs = configs;
        this.logs = logs;
        for (AiChatProvider provider : providers) {
            if (this.providers.put(provider.getProvider(), provider) != null) {
                throw new IllegalStateException("Duplicate AI provider: " + provider.getProvider());
            }
        }
    }

    @Override
    public AiChatResponse test(Integer configId, AiChatRequest request) {
        return test(configId, request, null);
    }

    @Override
    public AiChatResponse test(Integer configId, AiChatRequest request,
            AiStreamSession stream) {
        AiConfig config = new AiConfig();
        BeanUtils.copyProperties(configs.getUsableConfig(configId), config);
        return executeTest(config, request, stream);
    }

    @Override
    public AiChatResponse testDraft(AiConfig input, AiChatRequest request, AiStreamSession stream) {
        AiConfig config = configs.resolveConnection(input);
        config.setId(input.getId());
        config.setConfigName(StringUtils.left(StringUtils.trim(input.getConfigName()), 255));
        config.setDefaultModel(input.getDefaultModel());
        return executeTest(config, request, stream);
    }

    private AiChatResponse executeTest(AiConfig config, AiChatRequest request, AiStreamSession stream) {
        if (request == null || StringUtils.isBlank(request.getPrompt()) || request.getPrompt().length() > 16000) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        if (StringUtils.isNotBlank(request.getModel())) {
            String model = request.getModel().trim();
            if (model.length() > 255) {
                throw new AiException(400, "simple.ai.invalidConfig");
            }
            config.setDefaultModel(model);
        }
        if (StringUtils.isBlank(config.getDefaultModel()) || config.getDefaultModel().length() > 255) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        AiChatProvider provider = provider(config);
        request.setTrace(new AiCallTrace());
        AiLog entry = new AiLog();
        entry.setProvider(config.getProvider());
        entry.setConfigId(config.getId());
        entry.setConfigName(config.getConfigName());
        entry.setModel(config.getDefaultModel());
        entry.setPrompt(request.getPrompt());
        entry.setCreateDate(new Date());
        entry.setStatus(0);
        String owner = SecurityUtils.getLoginUserName();
        long start = System.nanoTime();
        String finishReason = "failed";
        try {
            AiChatResponse response = stream == null ? provider.chat(config, request) : provider.chat(config, request, stream);
            response.setDurationMs((System.nanoTime() - start) / 1000000);
            entry.setResponseText(StringUtils.left(response.getContent(), 16000));
            entry.setPromptTokens(response.getPromptTokens());
            entry.setCompletionTokens(response.getCompletionTokens());
            entry.setTotalTokens(response.getTotalTokens());
            entry.setStatus(1);
            finishReason = "success";
            return response;
        } catch (CancellationException e) {
            finishReason = "cancelled";
            entry.setErrorMessage(SimpleResultUtils.getErrorMsg("simple.ai.cancelled", null));
            throw e;
        } catch (AiException e) {
            entry.setErrorMessage(SimpleResultUtils.getErrorMsg(e.getMessageKey(), e.getArguments()));
            throw e;
        } catch (Exception e) {
            entry.setErrorMessage(SimpleResultUtils.getErrorMsg("simple.ai.requestFailed", null));
            throw new AiException(500, "simple.ai.requestFailed");
        } finally {
            if (stream != null && entry.getStatus() != 1) entry.setResponseText(StringUtils.left(stream.getText(), 16000));
            entry.setDurationMs((System.nanoTime() - start) / 1000000);
            entry.setRawResponse(request.getTrace().rawResponse());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("version", 1);
            detail.put("finishReason", finishReason);
            detail.put("streaming", stream != null);
            detail.put("httpStatus", request.getTrace().getHttpStatus());
            detail.put("requestBody", request.getTrace().getRequestBody());
            detail.put("truncated", request.getTrace().isTruncated());
            detail.put("usageComplete", entry.getPromptTokens() != null && entry.getCompletionTokens() != null && entry.getTotalTokens() != null);
            detail.put("steps", List.of(Map.of("type", "model", "round", 1, "status", finishReason, "duration", entry.getDurationMs())));
            entry.setExecutionDetail(JsonUtils.toJson(detail));
            if (stream == null) logs.record(entry);
            else logs.recordForUser(entry, owner);
        }
    }

    @Override
    public List<String> loadModels(Integer configId) {
        AiConfig config = configs.getUsableConfig(configId);
        return provider(config).loadModels(config);
    }

    @Override
    public List<String> loadModels(AiConfig input) {
        AiConfig config = configs.resolveConnection(input);
        return provider(config).loadModels(config);
    }

    private AiChatProvider provider(AiConfig config) {
        AiChatProvider provider = providers.get(AiProvider.fromCode(config.getProvider()));
        if (provider == null) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        return provider;
    }

}
