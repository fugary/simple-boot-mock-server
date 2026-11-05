package com.fugary.simple.mock.service.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    @JsonIgnore
    private AiCallTrace trace = new AiCallTrace();
    private String prompt;
    private String model;
    /** Optional saved configuration for help requests; ignored by configuration tests. */
    private Integer configId;
    /** Completed help turns, oldest first; ignored by configuration tests. */
    private List<HistoryTurn> history;

    @Data
    public static class HistoryTurn {
        private String question;
        private String answer;
    }
}
