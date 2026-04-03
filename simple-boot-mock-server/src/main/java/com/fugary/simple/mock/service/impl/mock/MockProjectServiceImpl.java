package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.mapper.mock.MockProjectMapper;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Create date 2024/7/15<br>
 *
 * @author gary.fu
 */
@Service
public class MockProjectServiceImpl extends ServiceImpl<MockProjectMapper, MockProject> implements MockProjectService {

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockProjectUserService mockProjectUserService;

    @Override
    public boolean deleteMockProject(Integer id) {
        MockProject mockProject = getById(id);
        if (mockProject != null) {
            List<MockGroup> mockGroups = mockGroupService.list(Wrappers.<MockGroup>query()
                    .eq("project_code", mockProject.getProjectCode())
                    .eq("user_name", mockProject.getUserName()));
            mockGroupService.deleteMockGroups(mockGroups.stream().map(MockGroup::getId).collect(Collectors.toList()));
        }
        return removeById(id);
    }

    @Override
    public boolean deleteMockProjects(List<Integer> ids) {
        for (Integer id : ids) {
            deleteMockProject(id);
        }
        return true;
    }

    @Override
    public boolean existsMockProject(MockProject project) {
        String projectCode = StringUtils.trimToNull(project.getProjectCode());
        if (projectCode == null) {
            return false;
        }
        return exists(Wrappers.<MockProject>query()
                .eq("project_code", projectCode)
                .ne(project.getId() != null, "id", project.getId()));
    }

    @Override
    public MockProject loadMockProject(String userName, String projectCode) {
        if (MockConstants.MOCK_DEFAULT_PROJECT.equals(projectCode)) {
            userName = "";
        }
        List<MockProject> existProjects = list(Wrappers.<MockProject>query().eq("user_name", userName)
                .eq("project_code", projectCode));
        if (existProjects.isEmpty()) {
            return null;
        }
        MockProject mockProject = existProjects.get(0);
        mockProject.setProjectUsers(mockProjectUserService.loadProjectUsers(mockProject.getId()));
        return mockProject;
    }

    @Override
    public boolean checkProjectValid(String userName, String projectCode) {
        if (MockConstants.MOCK_DEFAULT_PROJECT.equalsIgnoreCase(projectCode)) {
            return true;
        }
        return exists(Wrappers.<MockProject>query().eq("user_name", userName)
                .eq("project_code", projectCode)
                .eq("status", 1));
    }

    @Override
    public SimpleResult<MockProject> copyMockProject(Integer projectId, String userName) {
        MockProject mockProject = getById(projectId);
        if (mockProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        MockProject oldProject = SimpleMockUtils.copy(mockProject, MockProject.class);
        mockProject.setId(null);
        mockProject.setProjectCode(nextProjectCode());
        if (StringUtils.isNotBlank(userName) && !userName.equals(oldProject.getUserName())) {
            mockProject.setUserName(userName);
            mockProject.setPublicFlag(false);
        }
        mockProject.setProjectName(StringUtils.join(mockProject.getProjectName(), "-copy"));
        saveOrUpdate(mockProject);
        List<MockGroup> mockGroups = mockGroupService.list(Wrappers.<MockGroup>query()
                .eq("user_name", oldProject.getUserName())
                .eq("project_code", oldProject.getProjectCode()));
        for (MockGroup mockGroup : mockGroups) {
            mockGroupService.copyMockGroup(mockGroup.getId(), mockProject);
        }
        return SimpleResultUtils.createSimpleResult(mockProject);
    }

    @Override
    public SimpleResult<MockProject> saveMockProject(MockProject project) {
        if (project.getId() != null) {
            MockProject oldProject = getById(project.getId());
            if (oldProject == null) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
            }
            if (StringUtils.isBlank(project.getUserName())) {
                project.setUserName(oldProject.getUserName());
            }
            String projectCode = StringUtils.trimToNull(project.getProjectCode());
            project.setProjectCode(projectCode != null ? projectCode : nextProjectCode());
            if (!StringUtils.equals(project.getProjectCode(), oldProject.getProjectCode())
                    || !StringUtils.equals(project.getUserName(), oldProject.getUserName())) {
                mockGroupService.update(Wrappers.<MockGroup>update()
                        .set("user_name", project.getUserName())
                        .set("project_code", project.getProjectCode())
                        .eq("user_name", oldProject.getUserName())
                        .eq("project_code", oldProject.getProjectCode()));
                if (!StringUtils.equals(project.getProjectCode(), oldProject.getProjectCode())) {
                    mockProjectUserService.update(Wrappers.<MockProjectUser>update()
                            .set("project_code", project.getProjectCode())
                            .eq("project_id", oldProject.getId()));
                }
            }
        } else {
            project.setProjectCode(nextProjectCode());
        }
        saveOrUpdate(SimpleMockUtils.addAuditInfo(project));
        return SimpleResultUtils.createSimpleResult(project);
    }

    @Override
    public boolean hasProjectAuthority(String targetUserName, String projectCode, String authority) {
        if (SecurityUtils.validateUserUpdate(targetUserName)) {
            return true;
        }
        if (StringUtils.isBlank(projectCode) || MockConstants.MOCK_DEFAULT_PROJECT.equals(projectCode)) {
            return false;
        }
        String currentUserName = SecurityUtils.getLoginUserName();
        if (StringUtils.isBlank(currentUserName)) {
            return false;
        }
        MockProject project = loadMockProject(targetUserName, projectCode);
        if (project == null) {
            return false;
        }
        return mockProjectUserService.exists(Wrappers.<MockProjectUser>query()
                .eq("project_id", project.getId())
                .eq("user_name", currentUserName)
                .like(StringUtils.isNotBlank(authority), "authorities", authority));
    }

    private String nextProjectCode() {
        String projectCode = SimpleMockUtils.uuid();
        while (exists(Wrappers.<MockProject>query().eq("project_code", projectCode))) {
            projectCode = SimpleMockUtils.uuid();
        }
        return projectCode;
    }
}
