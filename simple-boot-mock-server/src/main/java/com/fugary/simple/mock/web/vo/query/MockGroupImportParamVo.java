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

    private static final long serialVersionUID = -6908379166190422653L;
    private String userName;
    private String type;
    private Integer duplicateStrategy;
    private String projectCode;
    private Boolean singleGroup; // 合并到单个组
    private String groupName;
}
