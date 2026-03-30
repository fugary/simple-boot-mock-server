package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Create date 2024/7/15<br>
 *
 * @author gary.fu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MockProject extends MockBase {

    private static final long serialVersionUID = -8012815542682860804L;
    private String userName;
    private String projectCode;
    private String projectName;
    private String description;
    private Boolean publicFlag;

    @TableField(exist = false)
    private java.util.List<MockProjectUser> projectUsers;
}
