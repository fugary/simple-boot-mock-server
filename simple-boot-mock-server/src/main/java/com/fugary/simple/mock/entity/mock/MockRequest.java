package com.fugary.simple.mock.entity.mock;

import lombok.Data;

/**
 * Created on 2020/5/3 22:28 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockRequest extends MockBase{

    private static final long serialVersionUID = 577338138715630744L;
    private Integer groupId;
    private String requestPath;
    private String method;
    private String matchPattern;
    private Integer delay;
    private String description;
    private String mockParams;

}
