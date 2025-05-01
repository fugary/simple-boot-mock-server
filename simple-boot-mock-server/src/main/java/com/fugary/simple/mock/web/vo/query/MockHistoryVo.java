package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Gary.Fu
 * @date 2025/5/1
 */
@Data
public class MockHistoryVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private Integer version;
}
