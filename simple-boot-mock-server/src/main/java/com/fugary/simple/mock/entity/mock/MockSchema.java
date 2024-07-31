package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Created on 2024/7/31 11:39 .<br>
 *
 * @author gary.fu
 */
@Data
@TableName(excludeProperty = "status")
public class MockSchema extends MockBase {

    private static final long serialVersionUID = 8517817442841283836L;
    private Integer requestId;
    private Integer dataId;
    private String parametersSchema;
    private String requestMediaType;
    private String requestBodySchema;
    private String responseMediaType;
    private String responseBodySchema;

}
