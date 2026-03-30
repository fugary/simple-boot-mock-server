package com.fugary.simple.mock.entity.mock;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fugary.simple.mock.contants.MockConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 协作成员
 * Create date 2026/03/30
 *
 * @author gary.fu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(excludeProperty = {"status", MockConstants.MODIFIER_KEY, MockConstants.MODIFY_DATE_KEY})
public class MockProjectUser extends MockBase {

    private static final long serialVersionUID = 1L;

    /**
     * 协作用户名
     */
    private String userName;

    /**
     * 项目代码
     */
    private String projectCode;

    /**
     * 操作权限(readable,writable,deletable等逗号分隔)
     */
    private String authorities;

}
