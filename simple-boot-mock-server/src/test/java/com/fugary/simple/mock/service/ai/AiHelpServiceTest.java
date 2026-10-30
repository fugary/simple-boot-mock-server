package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiHelpServiceTest {
    @Test
    void addsPackagedDocumentationAndUsesDefaultModelOnly() throws Exception {
        AiConfigService configs = mock(AiConfigService.class);
        AiService ai = mock(AiService.class);
        AiConfig config = new AiConfig();
        config.setId(7);
        when(configs.getDefaultConfig()).thenReturn(config);
        AiHelpService help = new AiHelpService(configs, ai);
        assertTrue(help.available());
        AiChatRequest question = new AiChatRequest();
        question.setPrompt("如何按订单轮询？");
        question.setModel("must-not-override-default");
        help.ask(question);
        ArgumentCaptor<AiChatRequest> sent = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai).test(eq(7), sent.capture());
        assertNull(sent.getValue().getModel());
        assertTrue(sent.getValue().getPrompt().contains("## 响应轮询"));
        assertTrue(sent.getValue().getPrompt().contains("request.params.orderId"));
        assertTrue(sent.getValue().getPrompt().contains(question.getPrompt()));
        assertTrue(sent.getValue().getPrompt().length() + 2000 < 16000);
        assertEquals("如何按订单轮询？", question.getPrompt());
    }

    @Test
    void rejectsMissingDefaultAndInvalidQuestionsWithoutCallingAi() throws Exception {
        AiConfigService configs = mock(AiConfigService.class);
        AiService ai = mock(AiService.class);
        AiHelpService help = new AiHelpService(configs, ai);
        assertFalse(help.available());
        assertThrows(AiException.class, () -> help.ask(null));
        AiChatRequest question = new AiChatRequest();
        question.setPrompt("  ");
        assertThrows(AiException.class, () -> help.ask(question));
        question.setPrompt("a".repeat(2001));
        assertThrows(AiException.class, () -> help.ask(question));
        question.setPrompt("如何使用 MockJS？");
        assertEquals("simple.ai.helpUnavailable", assertThrows(AiException.class,
            () -> help.ask(question)).getMessageKey());
        verifyNoInteractions(ai);
    }
}
