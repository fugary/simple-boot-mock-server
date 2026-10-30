package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.service.impl.ai.AiServiceImpl;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiServiceTest {
    @Test
    void recordsSuccessUsageAndSafeFailure() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        AiConfig config = new AiConfig();
        config.setProvider("openai");
        config.setStatus(1);
        config.setDefaultModel("model");
        when(configs.getOwned(1)).thenReturn(config);
        AiChatResponse response = new AiChatResponse();
        response.setContent("answer");
        response.setPromptTokens(2);
        response.setCompletionTokens(3);
        response.setTotalTokens(5);
        when(provider.chat(any(), any())).thenReturn(response)
            .thenThrow(new AiException(500, "simple.ai.httpError", 429));
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("hello");
        request.setModel("override-model");
        assertEquals("answer", service.test(1, request).getContent());
        assertEquals("model", config.getDefaultModel());
        AiException failure = assertThrows(AiException.class, () -> service.test(1, request));
        assertEquals("simple.ai.httpError", failure.getMessageKey());
        ArgumentCaptor<AiLog> captured = ArgumentCaptor.forClass(AiLog.class);
        verify(logs, times(2)).record(captured.capture());
        assertEquals(1, captured.getAllValues().get(0).getStatus());
        assertEquals("override-model", captured.getAllValues().get(0).getModel());
        assertEquals(5, captured.getAllValues().get(0).getTotalTokens());
        assertEquals(0, captured.getAllValues().get(1).getStatus());
        assertNotNull(captured.getAllValues().get(1).getErrorMessage());
    }

    @Test
    void accessDeniedBeforeProviderInvocation() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        when(configs.getOwned(1)).thenThrow(new AiException(404, "simple.error.code.404"));
        assertThrows(AiException.class, () -> service.test(1, new AiChatRequest()));
        verify(provider, never()).chat(any(), any());
        verifyNoInteractions(logs);
    }
}
