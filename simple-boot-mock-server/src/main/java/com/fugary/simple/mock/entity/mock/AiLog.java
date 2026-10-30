package com.fugary.simple.mock.entity.mock;

import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper = true)
public class AiLog extends MockBase {
    private String userName;
    private Integer configId;
    private String configName;
    private String model;
    private String prompt;
    private String responseText;
    private String errorMessage;
    private Long durationMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
