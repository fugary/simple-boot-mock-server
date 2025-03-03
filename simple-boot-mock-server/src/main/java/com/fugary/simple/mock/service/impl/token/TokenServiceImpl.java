package com.fugary.simple.mock.service.impl.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.mock.MockUserService;
import com.fugary.simple.mock.service.token.TokenService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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

    public static final String DEFAULT_ISSUER = "simple-mock";
    public static final String USER_NAME_KEY = "name";

    @Autowired
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @Autowired
    private MockUserService mockUserService;

    @Override
    public String createToken(MockUser user) {
        if (user != null) {
            Date currentDate = new Date();
            Date expireDate = DateUtils.addDays(currentDate, simpleMockConfigProperties.getJwtExpire());
            try {
                Algorithm algorithm = getAlgorithm();
                return JWT.create()
                        .withIssuer(DEFAULT_ISSUER)
                        .withClaim(USER_NAME_KEY, user.getUserName())
                        .withExpiresAt(expireDate)
                        .sign(algorithm);
            } catch (Exception e) {
                log.error("生成Jwt Token异常", e);
            }
        }
        return null;
    }

    @Override
    public MockUser fromAccessToken(String accessToken) {
        DecodedJWT decoded = getDecoded(accessToken);
        if (decoded != null) {
            return toMockUser(decoded);
        }
        return null;
    }

    @Override
    public SimpleResult<MockUser> validate(String accessToken) {
        int errorCode = MockErrorConstants.CODE_401;
        try {
            DecodedJWT decoded = getDecoded1(accessToken);
            MockUser mockUser = toMockUser(decoded);
            if (mockUser != null) {
                return SimpleResultUtils.createSimpleResult(mockUser);
            } else {
                errorCode = MockErrorConstants.CODE_404;
            }
        } catch (JWTVerificationException e) {
            // Invalid signature/claims
            log.error("Token Error", e);
        }
        return SimpleResultUtils.createSimpleResult(errorCode);
    }

    protected MockUser toMockUser(DecodedJWT decoded) {
        String userName = decoded.getClaim(USER_NAME_KEY).asString();
        return mockUserService.loadValidUser(userName);
    }

    protected DecodedJWT getDecoded(String accessToken) {
        try {
            return getDecoded1(accessToken);
        } catch (JWTVerificationException e) {
            // Invalid signature/claims
            log.error("Token验证没通过", e);
        }
        return null;
    }

    protected DecodedJWT getDecoded1(String accessToken) {
        JWTVerifier verifier = JWT.require(getAlgorithm())
                // specify an specific claim validations
                .withIssuer(DEFAULT_ISSUER)
                // reusable verifier instance
                .build();
        return verifier.verify(accessToken);
    }

    protected Algorithm getAlgorithm() {
        if (StringUtils.isBlank(simpleMockConfigProperties.getJwtPassword())) {
            simpleMockConfigProperties.setJwtPassword(RandomStringUtils.randomAlphanumeric(16));
        }
        return Algorithm.HMAC512(simpleMockConfigProperties.getJwtPassword());
    }
}
