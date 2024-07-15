package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
@Data
public class MockGroupExportParamVo implements Serializable {
    private static final long serialVersionUID = -7832449447785391000L;
    private String userName;
    private List<Integer> groupIds;
    private boolean exportAll;
    private String projectCode;
}

