package com.mengstudy.simple.mock.entity.mock;

import lombok.Data;

/**
 * Created on 2020/5/3 22:28 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockData extends MockBase {

    private static final long serialVersionUID = 8517817442841283836L;
    private Integer groupId;
    private Integer requestId;
    private Integer statusCode;
    private String contentType;
    private String responseBody;
    private String description;
    private String headers;
}
