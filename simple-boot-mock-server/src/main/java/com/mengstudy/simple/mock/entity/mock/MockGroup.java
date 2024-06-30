package com.mengstudy.simple.mock.entity.mock;

import lombok.Data;

/**
 * Created on 2020/5/3 22:27 .<br>
 *
 * @author gary.fu
 */
@Data
public class MockGroup extends MockBase {

    private static final long serialVersionUID = -8012815542682860804L;
    private String groupName;
    private String groupPath;
    private String proxyUrl;
    private String description;

}
