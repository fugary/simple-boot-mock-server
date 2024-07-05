package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.SimpleQueryVo;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.service.mock.MockGroupService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created on 2020/5/3 21:22 .<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/groups")
public class MockGroupController {

    @Autowired
    private MockGroupService mockGroupService;

    @GetMapping
    public SimpleResult<List<MockGroup>> search(@ModelAttribute SimpleQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockGroup> queryWrapper = Wrappers.query();
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("group_name", keyword)
                    .or().like("group_path", keyword));
        }
        return SimpleResultUtils.createSimpleResult(mockGroupService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockGroup> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockGroupService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockGroupService.deleteMockGroup(id));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockGroup group) {
        if (StringUtils.isBlank(group.getGroupPath())) {
            group.setGroupPath(SimpleMockUtils.uuid());
        }
        if (mockGroupService.existsMockGroup(group)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return SimpleResultUtils.createSimpleResult(mockGroupService.saveOrUpdate(SimpleMockUtils.addAuditInfo(group)));
    }
}
