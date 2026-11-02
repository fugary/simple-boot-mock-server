package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiHelpServiceTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void addsPackagedDocumentationAndUsesDefaultModelOnly(boolean streaming) throws Exception {
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
        AiStreamSession stream = streaming ? new AiStreamSession(text -> {}) : null;
        if (streaming) help.ask(question, stream);
        else help.ask(question);
        ArgumentCaptor<AiChatRequest> sent = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai).test(eq(7), sent.capture(), same(stream));
        assertNull(sent.getValue().getModel());
        assertTrue(sent.getValue().getPrompt().contains("当前项目"));
        assertFalse(sent.getValue().getPrompt().contains("Simple Boot Mock Server"));
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

    @Test
    void keepsRecentCompleteTurnsWithinBudgetWithoutRepeatingDocumentation() throws Exception {
        AiConfigService configs = mock(AiConfigService.class);
        AiService ai = mock(AiService.class);
        AiConfig config = new AiConfig();
        config.setId(7);
        when(configs.getDefaultConfig()).thenReturn(config);
        AiHelpService help = new AiHelpService(configs, ai);
        AiChatRequest question = new AiChatRequest();
        question.setPrompt("current-question");
        question.setHistory(java.util.List.of(turn("oldest", "answer"), turn("second", "answer"),
            turn("third", "answer"), turn("latest", "answer")));
        help.ask(question);
        question.setHistory(java.util.List.of(turn("older-large", "x".repeat(3900)), turn("latest", "answer")));
        help.ask(question);
        question.setHistory(java.util.List.of(turn("older", "answer"), turn("oversize", "x".repeat(4000))));
        help.ask(question);
        question.setHistory(java.util.Arrays.asList(turn("older", "answer"), null));
        help.ask(question);
        ArgumentCaptor<AiChatRequest> sent = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(ai, times(4)).test(eq(7), sent.capture(), isNull());
        String first = sent.getAllValues().get(0).getPrompt();
        assertFalse(first.contains("<user>oldest</user>"));
        assertTrue(first.indexOf("<user>second") < first.indexOf("<user>third"));
        assertTrue(first.indexOf("<user>third") < first.indexOf("<user>latest"));
        assertEquals(first.indexOf("<documentation>"), first.lastIndexOf("<documentation>"));
        String bounded = sent.getAllValues().get(1).getPrompt();
        assertFalse(bounded.contains("<user>older-large"));
        assertTrue(bounded.contains("<user>latest"));
        for (int i = 2; i < 4; i++) assertTrue(sent.getAllValues().get(i).getPrompt().contains("<history>\n</history>"));
        for (AiChatRequest request : sent.getAllValues()) assertTrue(request.getPrompt().length() <= 16000);
    }

    private AiChatRequest.HistoryTurn turn(String question, String answer) {
        AiChatRequest.HistoryTurn turn = new AiChatRequest.HistoryTurn();
        turn.setQuestion(question);
        turn.setAnswer(answer);
        return turn;
    }
}
