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
        String queryUserName = resolveQueryUserName(queryVo);
        if (queryVo.isPublicFlag()) {
            queryWrapper.eq("public_flag", true)
                    .eq(StringUtils.isNotBlank(queryUserName), "user_name", queryUserName)
                    .eq("status", 1);
        } else if (Boolean.TRUE.equals(queryVo.getOnlyMine())) {
            appendCurrentUserProjectCondition(queryWrapper);
        } else {
            appendProjectAuthorityCondition(queryWrapper, queryUserName);
        }
        applyProjectSort(queryWrapper);
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
        if (!mockProjectService.hasProjectAuthority(project.getUserName(), project.getId(),
                project.getProjectCode(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        if (project.getId() != null && mockProjectService.existsMockProject(project)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return mockProjectService.saveMockProject(project);
    }

    @GetMapping("/selectProjects")
    public SimpleResult<List<MockProject>> selectProjects(@ModelAttribute MockProjectQueryVo queryVo) {
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query();
        queryWrapper.eq("status", 1);
        String queryUserName = resolveQueryUserName(queryVo);
        if (queryVo.isPublicFlag()) {
            queryWrapper.eq("public_flag", true)
                    .eq("status", 1)
                    .eq(StringUtils.isNotBlank(queryUserName), "user_name", queryUserName);
        } else if (Boolean.TRUE.equals(queryVo.getOnlyMine())) {
            appendCurrentUserProjectCondition(queryWrapper);
        } else {
            appendProjectAuthorityCondition(queryWrapper, queryUserName);
        }
        applyProjectSort(queryWrapper);
        List<MockProject> projects = mockProjectService.list(queryWrapper);
        appendSelectedProject(projects, queryVo);
        populateProjectUsers(projects);
        return SimpleResultUtils.createSimpleResult(projects);
    }

    private String resolveQueryUserName(MockProjectQueryVo queryVo) {
        if (Boolean.TRUE.equals(queryVo.getOnlyMine())) {
            return SecurityUtils.getLoginUserName();
        }
        return StringUtils.trimToNull(queryVo.getUserName());
    }

    private void applyProjectSort(QueryWrapper<MockProject> queryWrapper) {
        String loginUserName = StringUtils.lowerCase(StringUtils.trimToEmpty(SecurityUtils.getLoginUserName()));
        StringBuilder sortBuilder = new StringBuilder("case when project_code = '")
                .append(MockConstants.MOCK_DEFAULT_PROJECT)
                .append("' then 0");
        if (StringUtils.isNotBlank(loginUserName)) {
            sortBuilder.append(" when lower(user_name) = '")
                    .append(StringUtils.replace(loginUserName, "'", "''"))
                    .append("' then 1");
        }
        sortBuilder.append(" else 2 end");
        queryWrapper.orderByAsc(sortBuilder.toString()).orderByDesc("id");
    }

    private void appendCurrentUserProjectCondition(QueryWrapper<MockProject> queryWrapper) {
        String loginUserName = SecurityUtils.getLoginUserName();
        queryWrapper.and(wrapper -> wrapper.eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT)
                .or(StringUtils.isNotBlank(loginUserName), userWrapper -> userWrapper.eq("user_name", loginUserName)));
    }

    private void appendProjectAuthorityCondition(QueryWrapper<MockProject> queryWrapper, String queryUserName) {
        final String loginUserName = SecurityUtils.getLoginUserName();
        final String targetUserName = StringUtils.trimToNull(queryUserName);
        final boolean canQueryTargetUser = StringUtils.isNotBlank(targetUserName)
                && SecurityUtils.validateUserUpdate(targetUserName);
        queryWrapper.and(wrapper -> {
            wrapper.eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT);
            if (canQueryTargetUser && StringUtils.equalsIgnoreCase(targetUserName, loginUserName)) {
                wrapper.or().eq("user_name", targetUserName)
                        .or().exists(buildProjectUserExistsSql(loginUserName));
            } else if (canQueryTargetUser) {
                wrapper.or().eq("user_name", targetUserName);
            } else if (StringUtils.isNotBlank(targetUserName) && StringUtils.isNotBlank(loginUserName)) {
                wrapper.or().and(projectWrapper -> projectWrapper.eq("user_name", targetUserName)
                        .exists(buildProjectUserExistsSql(loginUserName)));
            } else if (StringUtils.isNotBlank(loginUserName)) {
                wrapper.or().eq("user_name", loginUserName)
                        .or().exists(buildProjectUserExistsSql(loginUserName));
            }
        });
    }

    private String buildProjectUserExistsSql(String userName) {
        return "select 1 from t_mock_project_user pu where (pu.project_id = t_mock_project.id "
                + "or (pu.project_id is null and pu.project_code = t_mock_project.project_code)) and pu.user_name = '"
                + userName + "'";
    }

    private void populateProjectUsers(List<MockProject> projects) {
        if (projects != null) {
            for (MockProject project : projects) {
                project.setProjectUsers(mockProjectUserService.loadProjectUsers(project.getId(),
                        project.getProjectCode()));
            }
        }
    }

    private void appendSelectedProject(List<MockProject> projects, MockProjectQueryVo queryVo) {
        Integer projectId = queryVo.getProjectId();
        if (projectId == null) {
            return;
        }
        boolean existsProject = projects.stream().anyMatch(project -> projectId.equals(project.getId()));
        if (existsProject) {
            return;
        }
        MockProject project = mockProjectService.loadMockProject(resolveQueryUserName(queryVo), projectId, null);
        if (project == null || !project.isEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(queryVo.getOnlyMine())
                && !MockConstants.MOCK_DEFAULT_PROJECT.equalsIgnoreCase(StringUtils.trimToEmpty(project.getProjectCode()))
                && !SecurityUtils.isCurrentUser(project.getUserName())) {
            return;
        }
        boolean allowed = queryVo.isPublicFlag()
                ? Boolean.TRUE.equals(project.getPublicFlag())
                : mockProjectService.hasProjectAuthority(project.getUserName(), project.getId(),
                project.getProjectCode(), null);
        if (allowed) {
            projects.add(project);
        }
    }
}
