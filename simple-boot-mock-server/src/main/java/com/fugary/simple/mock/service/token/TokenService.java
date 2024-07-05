package com.fugary.simple.mock.service.token;

import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.web.vo.SimpleResult;

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

    /**
     * 获取用户
     * @param accessToken
     * @return
     */
    MockUser fromAccessToken(String accessToken);

    /**
     * 验证
     * @param accessToken
     * @return
     */
    SimpleResult<MockUser> validate(String accessToken);
}
