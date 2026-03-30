package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockProjectQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.fugary.simple.mock.utils.security.SecurityUtils.getLoginUser;

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

    @Autowired
    private MockProjectUserService mockProjectUserService;

    @GetMapping
    public SimpleResult<List<MockProject>> search(@ModelAttribute MockProjectQueryVo queryVo) {
        Page<MockProject> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus());
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        queryWrapper.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("project_name", keyword)
                .or().like("project_code", keyword)
                .or().like("description", keyword));
        String queryUserName = queryVo.getUserName();
        String userName = SecurityUtils.getUserName(queryUserName);
        if (queryVo.isPublicFlag()) {
            queryWrapper.eq("public_flag", true)
                    .eq(StringUtils.isNotBlank(queryUserName), "user_name", queryUserName)
                    .eq("status", 1);
        } else {
            queryWrapper.and(wrapper -> wrapper.and(wrapper1 -> wrapper1.eq("user_name", userName)
                    .or().eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT)
                    .or().exists("select 1 from t_mock_project_user pu where pu.project_code = t_mock_project.project_code and pu.user_name = '" + userName + "'")));
        }
        queryWrapper.orderByDesc("id");
        SimpleResult<List<MockProject>> result = SimpleResultUtils.createSimpleResult(mockProjectService.page(page, queryWrapper));
        populateProjectUsers(result.getResultData());
        return result;
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
        return SimpleResultUtils.createSimpleResult(mockProjectService.deleteMockProjects(ids));
    }

    @PostMapping("/copyMockProject/{projectId}")
    public SimpleResult<MockProject> copyMockProject(@PathVariable("projectId") Integer id,
            @RequestBody MockProjectQueryVo copyVo) {
        return mockProjectService.copyMockProject(id, copyVo.getUserName());
    }

    @PostMapping
    public SimpleResult<MockProject> save(@RequestBody MockProject project) {
        MockUser loginUser = getLoginUser();
        if (StringUtils.isBlank(project.getUserName()) && loginUser != null) {
            project.setUserName(loginUser.getUserName());
        }
        if (!mockProjectService.hasProjectAuthority(project.getUserName(), project.getProjectCode(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        if (mockProjectService.existsMockProject(project)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return mockProjectService.saveMockProject(project);
    }

    @GetMapping("/selectProjects")
    public SimpleResult<List<MockProject>> selectProjects(@ModelAttribute MockProjectQueryVo queryVo) {
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query();
        String userName = SecurityUtils.getUserName(queryVo.getUserName());
        queryWrapper.eq("status", 1);
        if (queryVo.isPublicFlag()) {
            queryWrapper.eq("public_flag", true).eq("status", 1);
        } else {
            queryWrapper.and(wrapper -> wrapper.and(wrapper1 -> wrapper1.eq("user_name", userName)
                    .or().eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT)
                    .or().exists("select 1 from t_mock_project_user pu where pu.project_code = t_mock_project.project_code and pu.user_name = '" + userName + "'")));
        }
        queryWrapper.orderByDesc("id");
        return SimpleResultUtils.createSimpleResult(mockProjectService.list(queryWrapper));
    }

    private void populateProjectUsers(List<MockProject> projects) {
        if (projects != null) {
            for (MockProject project : projects) {
                project.setProjectUsers(mockProjectUserService.loadProjectUsers(project.getProjectCode()));
            }
        }
    }
}
