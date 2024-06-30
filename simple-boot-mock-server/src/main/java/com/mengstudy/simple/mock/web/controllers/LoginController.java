package com.mengstudy.simple.mock.web.controllers;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mengstudy.simple.mock.contants.MockErrorConstants;
import com.mengstudy.simple.mock.entity.mock.MockUser;
import com.mengstudy.simple.mock.service.mock.MockUserService;
import com.mengstudy.simple.mock.service.token.TokenService;
import com.mengstudy.simple.mock.utils.SimpleResultUtils;
import com.mengstudy.simple.mock.web.vo.LoginResultVo;
import com.mengstudy.simple.mock.web.vo.SimpleResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class LoginController {

    @Autowired
    private MockUserService mockUserService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public SimpleResult<LoginResultVo> login(@RequestBody MockUser user) {
        MockUser loginUser = mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name",
                user.getUserName()));
        if (loginUser == null || !StringUtils.equalsIgnoreCase(user.getUserPassword(), loginUser.getUserPassword())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2001);
        }
        LoginResultVo resultVo = new LoginResultVo();
        resultVo.setAccount(loginUser);
        resultVo.setAccessToken(tokenService.createToken(loginUser));
        return SimpleResultUtils.createSimpleResult(resultVo);
    }
}
