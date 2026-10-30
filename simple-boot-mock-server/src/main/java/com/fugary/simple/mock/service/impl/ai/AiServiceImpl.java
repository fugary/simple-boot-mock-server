package com.fugary.simple.mock.service.impl.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.service.ai.AiConfigService;
import com.fugary.simple.mock.service.ai.AiLogService;
import com.fugary.simple.mock.service.ai.AiService;
import com.fugary.simple.mock.service.ai.AiProvider;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        AiConfig config = new AiConfig();
        BeanUtils.copyProperties(configs.getOwned(configId), config);
        if (!config.isEnabled() || request == null || StringUtils.isBlank(request.getPrompt()) || request.getPrompt().length() > 16000) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        if (StringUtils.isNotBlank(request.getModel())) {
            String model = request.getModel().trim();
            if (model.length() > 255) {
                throw new AiException(400, "simple.ai.invalidConfig");
            }
            config.setDefaultModel(model);
        }
        AiChatProvider provider = provider(config);
        AiLog entry = new AiLog();
        entry.setConfigId(configId);
        entry.setConfigName(config.getConfigName());
        entry.setModel(config.getDefaultModel());
        entry.setPrompt(request.getPrompt());
        entry.setCreateDate(new Date());
        entry.setStatus(0);
        long start = System.nanoTime();
        try {
            AiChatResponse response = provider.chat(config, request);
            response.setDurationMs((System.nanoTime() - start) / 1000000);
            entry.setResponseText(StringUtils.left(response.getContent(), 16000));
            entry.setPromptTokens(response.getPromptTokens());
            entry.setCompletionTokens(response.getCompletionTokens());
            entry.setTotalTokens(response.getTotalTokens());
            entry.setStatus(1);
            return response;
        } catch (AiException e) {
            entry.setErrorMessage(SimpleResultUtils.getErrorMsg(e.getMessageKey(), e.getArguments()));
            throw e;
        } catch (Exception e) {
            entry.setErrorMessage(SimpleResultUtils.getErrorMsg("simple.ai.requestFailed", null));
            throw new AiException(500, "simple.ai.requestFailed");
        } finally {
            entry.setDurationMs((System.nanoTime() - start) / 1000000);
            logs.record(entry);
        }
    }

    @Override
    public List<String> loadModels(Integer configId) {
        AiConfig config = configs.getOwned(configId);
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
