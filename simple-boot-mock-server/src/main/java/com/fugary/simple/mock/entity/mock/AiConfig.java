package com.fugary.simple.mock.entity.mock;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiConfig extends MockBase {
    private String userName;
    private String configName;
    private String provider;
    private String baseUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ToString.Exclude
    private String apiKey;
    private String defaultModel;
    private Integer isDefault;
}
