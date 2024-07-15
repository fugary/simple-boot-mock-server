package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;

/**
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
@Data
public class MockGroupImportParamVo implements Serializable {

    private String userName;
    private String type;
    private Integer duplicateStrategy;
    private String projectCode;
}
