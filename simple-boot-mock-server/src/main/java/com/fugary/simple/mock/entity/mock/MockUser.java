package com.fugary.simple.mock.entity.mock;

import lombok.Data;

/**
 * Created on 2020/5/5 18:37 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockUser extends MockBase {

    private static final long serialVersionUID = -1694029740474077017L;
    private String userName;
    private String userPassword;
    private String userEmail;
    private String nickName;
}
