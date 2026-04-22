package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Mock 请求复制参数
 *
 * @author gary.fu
 */
@Data
public class MockRequestCopyVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 目标场景编码，null 表示沿用原场景，空字符串表示默认场景
     */
    private String scenarioCode;

    private List<Integer> requestIds;

    private String action;
}
