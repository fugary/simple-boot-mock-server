package com.mengstudy.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mengstudy.simple.mock.contants.MockErrorConstants;
import com.mengstudy.simple.mock.entity.mock.MockUser;
import com.mengstudy.simple.mock.service.mock.MockUserService;
import com.mengstudy.simple.mock.service.token.TokenService;
import com.mengstudy.simple.mock.utils.SimpleResultUtils;
import com.mengstudy.simple.mock.web.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created on 2020/5/5 18:40 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private MockUserService mockUserService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public SimpleResult<String> login(@RequestBody MockUser user) {
        MockUser loginUser = mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name",
                user.getUserName()));
        if (loginUser == null || !StringUtils.equalsIgnoreCase(user.getUserPassword(), loginUser.getUserPassword())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2001);
        }
        return SimpleResultUtils.createSimpleResult(tokenService.createToken(loginUser));
    }

    @GetMapping("/info")
    public SimpleResult<MockUser> info(@RequestParam("token") String token) {
        DecodedJWT decode = JWT.decode(token);
        String name = decode.getClaim("name").asString();
        return SimpleResultUtils.createSimpleResult(mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name", name)));
    }

    @PostMapping("/logout")
    public SimpleResult logout() {
        return SimpleResultUtils.createSimpleResult(true);
    }
}
