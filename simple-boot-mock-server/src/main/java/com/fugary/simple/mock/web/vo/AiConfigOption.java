package com.fugary.simple.mock.web.vo;

import lombok.Data;

/** Safe configuration identity for selectors, including retained log history. */
@Data
public class AiConfigOption {
    private Integer id;
    private String configName;
    private String provider;
    private String baseUrl;
    private Integer isDefault;
    private Integer status;
    private boolean deleted;
}
