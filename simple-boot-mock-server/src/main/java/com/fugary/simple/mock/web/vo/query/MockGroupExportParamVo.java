package com.fugary.simple.mock.web.vo.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MockGroupExportParamVo extends MockGroupQueryVo {
    private static final long serialVersionUID = -7832449447785391000L;
    private List<Integer> groupIds;
    private boolean exportAll;
}

