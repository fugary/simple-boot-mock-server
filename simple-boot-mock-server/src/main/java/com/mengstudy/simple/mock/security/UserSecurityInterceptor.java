package com.mengstudy.simple.mock.security;

import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.contants.MockErrorConstants;
import com.mengstudy.simple.mock.entity.mock.MockUser;
import com.mengstudy.simple.mock.service.token.TokenService;
import com.mengstudy.simple.mock.utils.JsonUtils;
import com.mengstudy.simple.mock.utils.SimpleResultUtils;
import com.mengstudy.simple.mock.web.vo.SimpleResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 使用简单拦截器控制链接安全<br>
 * Create date 2023/6/27<br>
 *
 * @author gary.fu
 */
@Getter
@Setter
@Component
public class UserSecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isNotBlank(authorization)) {
            String accessToken = authorization.replaceFirst("Bearer ", StringUtils.EMPTY).trim();
            SimpleResult<MockUser> userResult = getTokenService().validate(accessToken);
            if (userResult.isSuccess()) {
                request.setAttribute(MockConstants.MOCK_USER_KEY, userResult.getResultData());
                return true;
            } else {
                responseJson(response, userResult);
            }
        }
        responseJson(response, SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_401));
        return false;
    }

    protected void responseJson(HttpServletResponse response, SimpleResult<MockUser> userResult) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (PrintWriter out = response.getWriter()) {
            out.write(JsonUtils.toJson(userResult));
            out.flush();
        }
    }

}
