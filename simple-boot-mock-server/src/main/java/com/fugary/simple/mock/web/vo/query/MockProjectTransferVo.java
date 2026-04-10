package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

/**
 * Mock 项目复制/移动参数
 *
 * @author gary.fu
 */
@Data
public class MockProjectTransferVo extends MockProjectQueryVo {

    private static final long serialVersionUID = -5526505738716087786L;

    private String action;
}
