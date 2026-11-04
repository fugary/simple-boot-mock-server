package com.fugary.simple.mock.entity.mock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiConfig extends MockBase implements HistoryBase {
    private Integer version;
    private Integer modifyFrom;
    private String userName;
    private String configName;
    private String provider;
    private String baseUrl;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ToString.Exclude
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String apiKey;
    private String defaultModel;
    private Integer isDefault;
}
