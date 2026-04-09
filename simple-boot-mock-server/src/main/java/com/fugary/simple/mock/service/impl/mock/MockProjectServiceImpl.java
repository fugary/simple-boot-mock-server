package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.mapper.mock.MockProjectMapper;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.service.mock.MockRequestService;
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

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockDataService mockDataService;

    @Override
    public boolean deleteMockProject(Integer id) {
        MockProject mockProject = getById(id);
        if (mockProject != null) {
            List<MockGroup> mockGroups = mockGroupService.list(buildProjectGroupQuery(mockProject));
            mockGroupService.deleteMockGroups(mockGroups.stream().map(MockGroup::getId).collect(Collectors.toList()));
            mockProjectUserService.remove(buildProjectUserQuery(mockProject));
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
        return loadMockProject(userName, null, projectCode);
    }

    @Override
    public MockProject loadMockProject(String userName, Integer projectId, String projectCode) {
        MockProject mockProject = null;
        if (projectId != null) {
            mockProject = getById(projectId);
        }
        String normalizedProjectCode = StringUtils.trimToNull(projectCode);
        if (mockProject == null && normalizedProjectCode != null) {
            QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query()
                    .eq("project_code", normalizedProjectCode);
            if (isDefaultProjectCode(normalizedProjectCode)) {
                queryWrapper.eq("user_name", "");
            } else if (StringUtils.isNotBlank(userName)) {
                queryWrapper.eq("user_name", userName);
            }
            mockProject = getOne(queryWrapper, false);
        }
        if (mockProject != null) {
            mockProject.setProjectUsers(mockProjectUserService.loadProjectUsers(mockProject.getId(),
                    mockProject.getProjectCode()));
        }
        return mockProject;
    }

    @Override
    public boolean checkProjectValid(String userName, String projectCode) {
        return checkProjectValid(userName, null, projectCode);
    }

    @Override
    public boolean checkProjectValid(String userName, Integer projectId, String projectCode) {
        if (projectId != null) {
            return exists(Wrappers.<MockProject>query().eq("id", projectId).eq("status", 1));
        }
        if (isDefaultProjectCode(projectCode)) {
            return true;
        }
        QueryWrapper<MockProject> queryWrapper = Wrappers.<MockProject>query()
                .eq("project_code", projectCode)
                .eq("status", 1);
        if (StringUtils.isNotBlank(userName)) {
            queryWrapper.eq("user_name", userName);
        }
        return exists(queryWrapper);
    }

    @Override
    public SimpleResult<MockProject> copyMockProject(Integer projectId, String userName) {
        MockProject mockProject = getById(projectId);
        if (mockProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        MockProject oldProject = SimpleMockUtils.copy(mockProject, MockProject.class);
        SimpleMockUtils.prepareForCreate(mockProject);
        mockProject.setProjectCode(nextProjectCode());
        if (StringUtils.isNotBlank(userName) && !userName.equals(oldProject.getUserName())) {
            mockProject.setUserName(userName);
            mockProject.setPublicFlag(false);
        }
        mockProject.setProjectName(StringUtils.join(mockProject.getProjectName(), "-copy"));
        saveOrUpdate(SimpleMockUtils.addAuditInfo(mockProject));
        List<MockGroup> mockGroups = mockGroupService.list(buildProjectGroupQuery(oldProject));
        for (MockGroup mockGroup : mockGroups) {
            mockGroupService.copyMockGroup(mockGroup, mockProject);
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
                        .set("project_id", oldProject.getId())
                        .set("project_code", project.getProjectCode())
                        .eq("user_name", oldProject.getUserName())
                        .and(wrapper -> wrapper.eq("project_id", oldProject.getId())
                                .or(legacy -> legacy.isNull("project_id")
                                        .eq("project_code", oldProject.getProjectCode()))));
                mockProjectUserService.update(Wrappers.<MockProjectUser>update()
                        .set("project_id", oldProject.getId())
                        .set("project_code", project.getProjectCode())
                        .and(wrapper -> wrapper.eq("project_id", oldProject.getId())
                                .or(legacy -> legacy.isNull("project_id")
                                        .eq("project_code", oldProject.getProjectCode()))));
            }
        } else {
            project.setProjectCode(nextProjectCode());
        }
        saveOrUpdate(SimpleMockUtils.addAuditInfo(project));
        return SimpleResultUtils.createSimpleResult(project);
    }

    @Override
    public boolean hasProjectAuthority(MockProject project, String authority) {
        if (project != null && !isDefaultProjectCode(project.getProjectCode())) {
            if (SecurityUtils.isAdminUser() || SecurityUtils.isCurrentUser(project.getUserName())) {
                return true;
            }
            String currentUserName = SecurityUtils.getLoginUserName();
            if (StringUtils.isBlank(currentUserName)) {
                return false;
            }
            return mockProjectUserService.exists(Wrappers.<MockProjectUser>query()
                    .eq("user_name", currentUserName)
                    .and(wrapper -> wrapper.eq("project_id", project.getId())
                            .or(legacy -> legacy.isNull("project_id")
                                    .eq("project_code", project.getProjectCode())))
                    .like(StringUtils.isNotBlank(authority), "authorities", authority));
        }
        return project != null && SecurityUtils.validateUserUpdate(project.getUserName());
    }

    @Override
    public boolean hasProjectAuthority(String targetUserName, Integer projectId, String projectCode, String authority) {
        MockProject project = loadMockProject(targetUserName, projectId, projectCode);
        if (project != null && !isDefaultProjectCode(project.getProjectCode())) {
            return hasProjectAuthority(project, authority);
        }
        return SecurityUtils.validateUserUpdate(targetUserName);
    }

    @Override
    public boolean hasGroupAuthority(Integer groupId, String authority) {
        if (groupId == null) {
            return false;
        }
        MockGroup group = mockGroupService.getById(groupId);
        return group != null && hasGroupAuthority(group, authority);
    }

    @Override
    public boolean hasGroupAuthority(MockGroup group, String authority) {
        return group != null && hasProjectAuthority(group.getUserName(), group.getProjectId(),
                group.getProjectCode(), authority);
    }

    @Override
    public boolean hasRequestAuthority(Integer requestId, String authority) {
        if (requestId == null) {
            return false;
        }
        MockRequest request = mockRequestService.getById(requestId);
        return request == null || hasRequestAuthority(request, authority);
    }

    @Override
    public boolean hasRequestAuthority(MockRequest request, String authority) {
        return request != null && hasGroupAuthority(request.getGroupId(), authority);
    }

    @Override
    public boolean hasDataAuthority(Integer dataId, String authority) {
        if (dataId == null) {
            return false;
        }
        MockData data = mockDataService.getById(dataId);
        return data == null || hasDataAuthority(data, authority);
    }

    @Override
    public boolean hasDataAuthority(MockData data, String authority) {
        return data != null && hasRequestAuthority(data.getRequestId(), authority);
    }

    private QueryWrapper<MockGroup> buildProjectGroupQuery(MockProject project) {
        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .eq("user_name", project.getUserName());
        if (isDefaultProjectCode(project.getProjectCode())) {
            queryWrapper.eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT);
        } else {
            queryWrapper.and(wrapper -> wrapper.eq("project_id", project.getId())
                    .or(legacy -> legacy.isNull("project_id")
                            .eq("project_code", project.getProjectCode())));
        }
        return queryWrapper;
    }

    private QueryWrapper<MockProjectUser> buildProjectUserQuery(MockProject project) {
        QueryWrapper<MockProjectUser> queryWrapper = Wrappers.<MockProjectUser>query();
        if (isDefaultProjectCode(project.getProjectCode())) {
            queryWrapper.eq("project_code", MockConstants.MOCK_DEFAULT_PROJECT);
        } else {
            queryWrapper.and(wrapper -> wrapper.eq("project_id", project.getId())
                    .or(legacy -> legacy.isNull("project_id")
                            .eq("project_code", project.getProjectCode())));
        }
        return queryWrapper;
    }

    private boolean isDefaultProjectCode(String projectCode) {
        return StringUtils.equalsIgnoreCase(MockConstants.MOCK_DEFAULT_PROJECT, StringUtils.trimToEmpty(projectCode));
    }

    private String nextProjectCode() {
        String projectCode = SimpleMockUtils.uuid();
        while (exists(Wrappers.<MockProject>query().eq("project_code", projectCode))) {
            projectCode = SimpleMockUtils.uuid();
        }
        return projectCode;
    }
}
