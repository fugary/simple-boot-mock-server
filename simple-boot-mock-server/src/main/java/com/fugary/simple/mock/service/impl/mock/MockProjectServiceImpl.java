package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.mapper.mock.MockProjectMapper;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
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
        return false;
    }

    @Override
    public boolean existsMockProject(MockProject project) {
        if (MockConstants.MOCK_DEFAULT_PROJECT.equalsIgnoreCase(project.getProjectCode())) {
            return true;
        }
        List<MockProject> existProjects = list(Wrappers.<MockProject>query().eq("user_name", project.getUserName())
                .eq("project_code", project.getProjectCode()));
        return existProjects.stream().anyMatch(existProject -> !existProject.getId().equals(project.getId()));
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
}
