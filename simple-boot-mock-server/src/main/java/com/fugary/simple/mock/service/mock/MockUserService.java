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
     * 级联删除分组和请求和数据
     *
     * @param id
     * @return
     */
    boolean deleteMockUser(Integer id);

    /**
     * 检查是否有重复
     *
     * @param user
     * @return
     */
    boolean existsUser(MockUser user);

    /**
     * 获取正常用户
     *
     * @param userName
     * @return
     */
    MockUser loadValidUser(String userName);

    /**
     * 加密密码
     * @param password
     * @return
     */
    String encryptPassword(String password);

    /**
     * 密码匹配
     * @param password
     * @param encryptPassword
     * @return
     */
    boolean matchPassword(String password, String encryptPassword);

    /**
     * 更新用户名
     *
     * @param user
     * @param existUser
     */
    void updateUserName(MockUser user, MockUser existUser);
}
