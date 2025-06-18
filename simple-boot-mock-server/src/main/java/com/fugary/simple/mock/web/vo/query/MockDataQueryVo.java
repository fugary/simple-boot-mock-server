package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

/**
 * Created on 2020/5/4 22:45 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockDataQueryVo extends SimpleQueryVo {

    private static final long serialVersionUID = -8998529400117184588L;
    private Integer requestId;
    private String statusCode;
}
