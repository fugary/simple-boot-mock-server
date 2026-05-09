package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockProjectUserBatchVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 协作成员管理
 * Create date 2026/03/30
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/projectUsers")
public class MockProjectUserController {

    @Autowired
    private MockProjectUserService mockProjectUserService;

    @Autowired
    private MockProjectService mockProjectService;

    /**
     * 查询某项目的协作成员列表
     *
     * @param projectId 项目ID
     * @return
     */
    @GetMapping
    public SimpleResult<List<MockProjectUser>> search(@RequestParam("projectId") Integer projectId) {
        return SimpleResultUtils.createSimpleResult(mockProjectUserService.loadProjectUsers(projectId));
    }

    /**
     * 保存协作成员
     *
     * @param projectUser
     * @return
     */
    @PostMapping
    public SimpleResult<MockProjectUser> save(@RequestBody MockProjectUser projectUser) {
        if (projectUser == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        if (projectUser.getId() != null) {
            return updateAuthorities(projectUser);
        }
        if (projectUser.getProjectId() == null || StringUtils.isBlank(projectUser.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        SimpleResult<MockProject> projectResult = validateSaveProject(projectUser.getProjectId(), null);
        if (!projectResult.isSuccess()) {
            return SimpleResultUtils.createSimpleResult(projectResult.getCode(), null);
        }
        MockProject project = projectResult.getResultData();
        if (isInvalidProjectUser(project, projectUser.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        projectUser.setProjectCode(project.getProjectCode());
        boolean exists = mockProjectUserService.exists(Wrappers.<MockProjectUser>query()
                .eq("project_id", projectUser.getProjectId())
                .eq("user_name", projectUser.getUserName()));
        if (exists) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001, null);
        }
        SimpleMockUtils.addAuditInfo(projectUser);
        mockProjectUserService.saveOrUpdate(projectUser);
        return SimpleResultUtils.createSimpleResult(projectUser);
    }

    /**
     * Batch save project users.
     *
     * @param projectUserBatchVo
     * @return
     */
    @PostMapping("/batch")
    public SimpleResult<List<MockProjectUser>> saveBatch(@RequestBody MockProjectUserBatchVo projectUserBatchVo) {
        if (projectUserBatchVo == null || projectUserBatchVo.getProjectId() == null
                || CollectionUtils.isEmpty(projectUserBatchVo.getUserNames())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        List<String> userNames = projectUserBatchVo.getUserNames().stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userNames)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        SimpleResult<MockProject> projectResult = validateSaveProject(projectUserBatchVo.getProjectId(), null);
        if (!projectResult.isSuccess()) {
            return SimpleResultUtils.createSimpleResult(projectResult.getCode(), null);
        }
        MockProject project = projectResult.getResultData();
        if (userNames.stream().anyMatch(userName -> isInvalidProjectUser(project, userName))) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        boolean exists = mockProjectUserService.exists(Wrappers.<MockProjectUser>query()
                .eq("project_id", projectUserBatchVo.getProjectId())
                .in("user_name", userNames));
        if (exists) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001, null);
        }
        List<MockProjectUser> projectUsers = userNames.stream().map(userName -> {
            MockProjectUser projectUser = new MockProjectUser();
            projectUser.setProjectId(projectUserBatchVo.getProjectId());
            projectUser.setProjectCode(project.getProjectCode());
            projectUser.setUserName(userName);
            projectUser.setAuthorities(projectUserBatchVo.getAuthorities());
            return SimpleMockUtils.addAuditInfo(projectUser);
        }).collect(Collectors.toList());
        mockProjectUserService.saveBatch(projectUsers);
        return SimpleResultUtils.createSimpleResult(projectUsers);
    }

    /**
     * 删除协作成员
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public SimpleResult<Boolean> remove(@PathVariable("id") Integer id) {
        MockProjectUser projectUser = mockProjectUserService.getById(id);
        if (projectUser == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, false);
        }
        MockProject project = projectUser.getProjectId() != null
                ? mockProjectService.getById(projectUser.getProjectId())
                : mockProjectService.loadMockProject(null, projectUser.getProjectId(), projectUser.getProjectCode());
        if (project == null || !SecurityUtils.validateUserUpdate(project.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, false);
        }
        return SimpleResultUtils.createSimpleResult(mockProjectUserService.removeById(id));
    }

    private SimpleResult<MockProjectUser> updateAuthorities(MockProjectUser projectUser) {
        MockProjectUser existProjectUser = mockProjectUserService.getById(projectUser.getId());
        if (existProjectUser == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, null);
        }
        SimpleResult<MockProject> projectResult = validateSaveProject(existProjectUser.getProjectId(),
                existProjectUser.getProjectCode());
        if (!projectResult.isSuccess()) {
            return SimpleResultUtils.createSimpleResult(projectResult.getCode(), null);
        }
        MockProject project = projectResult.getResultData();
        existProjectUser.setProjectId(project.getId());
        existProjectUser.setProjectCode(project.getProjectCode());
        existProjectUser.setAuthorities(projectUser.getAuthorities());
        SimpleMockUtils.addAuditInfo(existProjectUser);
        mockProjectUserService.saveOrUpdate(existProjectUser);
        return SimpleResultUtils.createSimpleResult(existProjectUser);
    }

    private SimpleResult<MockProject> validateSaveProject(Integer projectId, String projectCode) {
        if (projectId == null && StringUtils.isBlank(projectCode)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        MockProject project = projectId != null
                ? mockProjectService.getById(projectId)
                : mockProjectService.loadMockProject(null, projectId, projectCode);
        if (project == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, null);
        }
        if (StringUtils.equals(project.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        if (!SecurityUtils.validateUserUpdate(project.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, null);
        }
        return SimpleResultUtils.createSimpleResult(project);
    }

    private boolean isInvalidProjectUser(MockProject project, String userName) {
        return StringUtils.equals(project.getUserName(), userName) || SecurityUtils.isCurrentUser(userName);
    }

}
