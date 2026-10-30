package com.fugary.simple.mock.web.vo.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Configuration-specific filters; inherited keyword matches the configuration name. */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiConfigQueryVo extends SimpleQueryVo {
    private String provider;
    private String defaultModel;
}
