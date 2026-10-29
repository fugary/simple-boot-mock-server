package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * Created on 2020/5/3 22:28 .<br>
 *
 * @author gary.fu
 */
@Data
@FieldNameConstants
public class MockData extends MockBase implements HistoryBase {

    private static final long serialVersionUID = 8517817442841283836L;
    private Integer groupId;
    private Integer requestId;
    private String dataName;
    private Integer statusCode;
    private Integer defaultFlag;
    private String contentType;
    private String matchPattern;
    private String responseFormat;
    private String responseBody;
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String responseBodyPreview;
    private Integer delay;
    private String description;
    private String headers;
    private String mockParams;
    private String defaultCharset;
    private String postProcessor;
    @Version
    @TableField("data_version")
    private Integer version;
    private Integer modifyFrom;

}
