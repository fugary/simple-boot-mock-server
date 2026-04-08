package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.util.List;

/**
 * Mock 分组复制/移动参数
 *
 * @author gary.fu
 */
@Data
public class MockGroupTransferVo extends MockGroupQueryVo {

    private static final long serialVersionUID = 3950863939387267876L;

    private List<Integer> groupIds;

    private String action;
}
