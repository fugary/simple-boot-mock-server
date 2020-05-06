package com.mengstudy.simple.mock.service.impl.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.mengstudy.simple.mock.config.SimpleMockConfigProperties;
import com.mengstudy.simple.mock.entity.mock.MockUser;
import com.mengstudy.simple.mock.service.token.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Created on 2020/5/5 20:46 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class TokenServiceImpl implements TokenService {

    @Autowired
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @Override
    public String createToken(MockUser user) {
        if (user != null) {
            Date currentDate = new Date();
            Date expireDate = DateUtils.addDays(currentDate, simpleMockConfigProperties.getJwtExpire());
            try {
                Algorithm algorithm = Algorithm.HMAC256(simpleMockConfigProperties.getJwtPassword());
                return JWT.create()
                        .withClaim("name", user.getUserName())
                        .withExpiresAt(expireDate)
                        .sign(algorithm);
            } catch (Exception e) {
                log.error("生成Jwt Token异常", e);
            }
        }
        return null;
    }
}
