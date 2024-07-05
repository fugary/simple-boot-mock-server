package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
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
        QueryWrapper<MockUser> queryWrapper = Wrappers.query();
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("user_name", keyword)
                    .or().like("nick_name", keyword));
        }
        return SimpleResultUtils.createSimpleResult(mockUserService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockUser> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockUserService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockUserService.removeById(id));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockUser user) {
        if (mockUserService.existsUser(user)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return SimpleResultUtils.createSimpleResult(mockUserService.saveOrUpdate(SimpleMockUtils.addAuditInfo(user)));
    }

    @GetMapping("/info")
    public SimpleResult<MockUser> info(@RequestParam("token") String token) {
        DecodedJWT decode = JWT.decode(token);
        String name = decode.getClaim("name").asString();
        return SimpleResultUtils.createSimpleResult(mockUserService.getOne(Wrappers.<MockUser>query().eq("user_name", name)));
    }
}
