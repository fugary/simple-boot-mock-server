package com.fugary.simple.mock.service.ai.provider;

import lombok.Data;
import java.util.List;

@Data
public class AiChatRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private AiCallTrace trace = new AiCallTrace();
    private String prompt;
    private String model;
    /** Completed help turns, oldest first; ignored by configuration tests. */
    private List<HistoryTurn> history;

    @Data
    public static class HistoryTurn {
        private String question;
        private String answer;
    }
}
