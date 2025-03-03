package com.fugary.simple.mock.web.controllers;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.mock.MockUserService;
import com.fugary.simple.mock.service.token.TokenService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.LoginResultVo;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.h2.console.enabled:false}")
    private boolean consoleEnabled;

    @PostMapping("/login")
    public SimpleResult<LoginResultVo> login(@RequestBody MockUser user) {
        MockUser loginUser = mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name",
                user.getUserName()).eq("status", 1));
        if (loginUser == null || !mockUserService.matchPassword(user.getUserPassword(), loginUser.getUserPassword())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2001);
        }
        LoginResultVo resultVo = new LoginResultVo();
        resultVo.setAccount(loginUser);
        resultVo.setAccessToken(tokenService.createToken(loginUser));
        resultVo.setConsoleEnabled(consoleEnabled);
        return SimpleResultUtils.createSimpleResult(resultVo);
    }
}
