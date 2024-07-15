package com.fugary.simple.mock.entity.mock;

import lombok.Data;

/**
 * Create date 2024/7/15<br>
 *
 * @author gary.fu
 */
@Data
public class MockProject extends MockBase {

    private static final long serialVersionUID = -8012815542682860804L;
    private String userName;
    private String projectCode;
    private String projectName;
}
