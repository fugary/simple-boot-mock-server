package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.SimpleQueryVo;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.mock.MockUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created on 2020/5/5 18:40 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private MockUserService mockUserService;

    @GetMapping
    public SimpleResult<List<MockUser>> search(@ModelAttribute SimpleQueryVo queryVo) {
        Page<MockUser> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockUser> queryWrapper = Wrappers.<MockUser>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus());
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("user_name", keyword)
                    .or().like("nick_name", keyword)
                    .or().like("user_email", keyword));
        }
        return SimpleResultUtils.createSimpleResult(mockUserService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockUser> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockUserService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockUserService.deleteMockUser(id));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockUser user) {
        if (mockUserService.existsUser(user)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        if (!SecurityUtils.validateUserUpdate(user.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        boolean needEncryptPassword= true;
        MockUser existUser = null;
        if (user.getId() != null) {
            existUser = mockUserService.getById(user.getId());
            needEncryptPassword = existUser == null || !StringUtils.equalsIgnoreCase(existUser.getUserPassword(), user.getUserPassword());
        }
        if (needEncryptPassword) {
            user.setUserPassword(mockUserService.encryptPassword(user.getUserPassword()));
        }
        boolean saveResult = mockUserService.saveOrUpdate(SimpleMockUtils.addAuditInfo(user));
        if (saveResult) {
            mockUserService.updateUserName(user, existUser);
        }
        return SimpleResultUtils.createSimpleResult(saveResult);
    }

    @GetMapping("/info")
    public SimpleResult<MockUser> info(@RequestParam("token") String token) {
        DecodedJWT decode = JWT.decode(token);
        String name = decode.getClaim("name").asString();
        return SimpleResultUtils.createSimpleResult(mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name", name)));
    }
}
