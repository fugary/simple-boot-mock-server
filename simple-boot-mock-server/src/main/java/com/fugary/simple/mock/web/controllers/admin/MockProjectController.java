package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockProjectQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Create date 2024/7/15<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/projects")
public class MockProjectController {

    @Autowired
    private MockProjectService mockProjectService;

    @GetMapping
    public SimpleResult<List<MockProject>> search(@ModelAttribute MockProjectQueryVo queryVo) {
        Page<MockProject> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query();
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("project_name", keyword)
                    .or().like("project_code", keyword));
        }
        String userName = SecurityUtils.getUserName(queryVo.getUserName());
        queryWrapper.eq("user_name", userName);
        return SimpleResultUtils.createSimpleResult(mockProjectService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockProject> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockProjectService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockProjectService.deleteMockProject(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockProjectService.removeByIds(ids));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockProject project) {
        if (mockProjectService.existsMockProject(project)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return SimpleResultUtils.createSimpleResult(mockProjectService.saveOrUpdate(SimpleMockUtils.addAuditInfo(project)));
    }

    @PostMapping("/selectProjects")
    public SimpleResult<List<MockProject>> selectProjects(@RequestBody MockProjectQueryVo queryVo) {
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query();
        String userName = SecurityUtils.getUserName(queryVo.getUserName());
        queryWrapper.and(wrapper -> wrapper.eq("user_name", userName)
                .or().eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT));
        return SimpleResultUtils.createSimpleResult(mockProjectService.list(queryWrapper));
    }
}
