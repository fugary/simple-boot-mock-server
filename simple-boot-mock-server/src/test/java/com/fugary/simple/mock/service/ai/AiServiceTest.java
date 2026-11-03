package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;
import com.fugary.simple.mock.service.impl.ai.AiServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiServiceTest {
    @Test
    void draftUsesUnsavedConnectionAndModelWithoutSaving() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        AiConfig draft = new AiConfig();
        draft.setConfigName("draft");
        draft.setDefaultModel("draft-model");
        draft.setStatus(0);
        AiConfig connection = new AiConfig();
        connection.setProvider("openai");
        connection.setBaseUrl("http://localhost:1234/v1");
        connection.setApiKey("draft-key");
        when(configs.resolveConnection(draft)).thenReturn(connection);
        AiChatResponse response = new AiChatResponse();
        response.setContent("draft answer");
        when(provider.chat(any(), any())).thenReturn(response);
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("hello");
        request.setModel("temporary-model");
        assertEquals("draft answer", service.testDraft(draft, request, null).getContent());
        ArgumentCaptor<AiConfig> sent = ArgumentCaptor.forClass(AiConfig.class);
        verify(provider).chat(sent.capture(), eq(request));
        assertEquals("http://localhost:1234/v1", sent.getValue().getBaseUrl());
        assertEquals("draft-key", sent.getValue().getApiKey());
        assertEquals("temporary-model", sent.getValue().getDefaultModel());
        assertEquals("draft-model", draft.getDefaultModel());
        ArgumentCaptor<AiLog> recorded = ArgumentCaptor.forClass(AiLog.class);
        verify(logs).record(recorded.capture());
        assertNull(recorded.getValue().getConfigId());
        assertEquals("draft", recorded.getValue().getConfigName());
        verify(configs).resolveConnection(draft);
        verifyNoMoreInteractions(configs);
    }

    @Test
    void draftAccessDeniedBeforeProviderInvocation() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        AiConfig draft = new AiConfig();
        when(configs.resolveConnection(draft)).thenThrow(new AiException(403, "simple.error.code.403"));
        assertThrows(AiException.class, () -> service.testDraft(draft, new AiChatRequest(), null));
        verify(provider, never()).chat(any(), any());
        verifyNoInteractions(logs);
    }

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
        when(configs.getUsableConfig(1)).thenReturn(config);
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
        assertEquals("openai", captured.getAllValues().get(0).getProvider());
        assertTrue(captured.getAllValues().get(0).getExecutionDetail().contains("success"));
        assertTrue(captured.getAllValues().get(1).getExecutionDetail().contains("failed"));
        assertEquals("override-model", captured.getAllValues().get(0).getModel());
        assertEquals(5, captured.getAllValues().get(0).getTotalTokens());
        assertEquals(0, captured.getAllValues().get(1).getStatus());
        assertNotNull(captured.getAllValues().get(1).getErrorMessage());
    }

    @Test
    void cancellationKeepsPartialAnswerAndOwnerAfterRequestCloses() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        AiConfig config = new AiConfig();
        config.setProvider("openai");
        config.setStatus(1);
        config.setDefaultModel("model");
        when(configs.getUsableConfig(1)).thenReturn(config);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockUser user = new MockUser();
        user.setUserName("stream-user");
        servletRequest.setAttribute(MockConstants.MOCK_USER_KEY, user);
        RequestContextHolder.setRequestAttributes(
            new ServletRequestAttributes(servletRequest));
        AiStreamSession stream =
            new AiStreamSession(text -> {});
        when(provider.chat(any(), any(), any())).thenAnswer(call -> {
            ((AiChatRequest) call.getArgument(1)).getTrace().append("partial upstream event");
            stream.text("partial");
            servletRequest.removeAttribute(MockConstants.MOCK_USER_KEY);
            stream.cancel();
            stream.check();
            return null;
        });
        try {
            AiChatRequest request = new AiChatRequest();
            request.setPrompt("hello");
            assertThrows(CancellationException.class, () -> service.test(1, request, stream));
            ArgumentCaptor<AiLog> captured = ArgumentCaptor.forClass(AiLog.class);
            verify(logs).recordForUser(captured.capture(), eq("stream-user"));
            assertEquals("partial", captured.getValue().getResponseText());
            assertEquals("partial upstream event", captured.getValue().getRawResponse());
            assertTrue(captured.getValue().getExecutionDetail().contains("cancelled"));
            assertEquals(0, captured.getValue().getStatus());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void accessDeniedBeforeProviderInvocation() {
        AiConfigService configs = mock(AiConfigService.class);
        AiLogService logs = mock(AiLogService.class);
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configs, logs, List.of(provider));
        when(configs.getUsableConfig(1)).thenThrow(new AiException(404, "simple.error.code.404"));
        assertThrows(AiException.class, () -> service.test(1, new AiChatRequest()));
        verify(provider, never()).chat(any(), any());
        verifyNoInteractions(logs);
    }
}
