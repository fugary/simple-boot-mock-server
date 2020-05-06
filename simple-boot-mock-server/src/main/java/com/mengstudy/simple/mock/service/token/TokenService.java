package com.mengstudy.simple.mock.service.token;

import com.mengstudy.simple.mock.entity.mock.MockUser;

/**
 * Created on 2020/5/5 20:41 .<br>
 *
 * @author gary.fu
 */
public interface TokenService {

    /**
     * 生成Token
     *
     * @param user
     * @return
     */
    String createToken(MockUser user);
}
