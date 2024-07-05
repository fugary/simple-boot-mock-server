package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockUser;

/**
 * Created on 2020/5/5 22:36 .<br>
 *
 * @author gary.fu
 */
public interface MockUserService extends IService<MockUser> {

    /**
     * 检查是否有重复
     *
     * @param user
     * @return
     */
    boolean existsUser(MockUser user);

}
