package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Mock project user batch save params.
 *
 * @author gary.fu
 */
@Data
public class MockProjectUserBatchVo implements Serializable {

    private static final long serialVersionUID = -2645079081890553404L;

    private Integer projectId;

    private List<String> userNames;

    private String authorities;
}
