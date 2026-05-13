package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockProjectServiceImplTest {

    @Mock
    private MockGroupService mockGroupService;

    @Mock
    private MockProjectUserService mockProjectUserService;

    @Mock
    private MockRequestService mockRequestService;

    @Mock
    private MockDataService mockDataService;

    private MockProjectServiceImpl mockProjectService;

    @BeforeEach
    void setUp() {
        mockProjectService = spy(new MockProjectServiceImpl());
        ReflectionTestUtils.setField(mockProjectService, "mockGroupService", mockGroupService);
        ReflectionTestUtils.setField(mockProjectService, "mockProjectUserService", mockProjectUserService);
        ReflectionTestUtils.setField(mockProjectService, "mockRequestService", mockRequestService);
        ReflectionTestUtils.setField(mockProjectService, "mockDataService", mockDataService);
        doReturn(false).when(mockProjectService).exists(any());
        doAnswer(invocation -> SimpleResultUtils.createSimpleResult((MockProject) invocation.getArgument(0)))
                .when(mockProjectService).saveMockProject(any(MockProject.class));
    }

    @Test
    void copyMockProjectShouldOnlyCopyCurrentGroups() {
        MockProject sourceProject = createProject();
        MockGroup currentGroup = createGroup(11, null);
        MockGroup historyGroup = createGroup(12, currentGroup.getId());
        doReturn(sourceProject).when(mockProjectService).getById(sourceProject.getId());
        when(mockGroupService.list(anyGroupQueryWrapper())).thenAnswer(invocation -> {
            QueryWrapper<MockGroup> queryWrapper = invocation.getArgument(0);
            if (StringUtils.containsIgnoreCase(queryWrapper.getSqlSegment(),
                    MockConstants.DB_MODIFY_FROM_KEY + " IS NULL")) {
                return List.of(currentGroup);
            }
            return List.of(currentGroup, historyGroup);
        });
        when(mockGroupService.copyMockGroup(any(MockGroup.class), any(MockProject.class)))
                .thenReturn(SimpleResultUtils.createSimpleResult(new MockGroup()));

        SimpleResult<MockProject> result = mockProjectService.copyMockProject(sourceProject.getId(),
                sourceProject.getUserName());

        ArgumentCaptor<QueryWrapper<MockGroup>> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mockGroupService).list(queryCaptor.capture());
        assertTrue(result.isSuccess());
        assertTrue(StringUtils.containsIgnoreCase(queryCaptor.getValue().getSqlSegment(),
                MockConstants.DB_MODIFY_FROM_KEY + " IS NULL"));
        verify(mockGroupService).copyMockGroup(currentGroup, result.getResultData());
        verify(mockGroupService, never()).copyMockGroup(historyGroup, result.getResultData());
    }

    private MockProject createProject() {
        MockProject project = new MockProject();
        project.setId(1);
        project.setUserName("gary");
        project.setProjectCode("project-code");
        project.setProjectName("project-name");
        project.setStatus(1);
        return project;
    }

    private MockGroup createGroup(Integer id, Integer modifyFrom) {
        MockGroup group = new MockGroup();
        group.setId(id);
        group.setUserName("gary");
        group.setProjectId(1);
        group.setProjectCode("project-code");
        group.setGroupPath("group-" + id);
        group.setGroupName("Group " + id);
        group.setModifyFrom(modifyFrom);
        return group;
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<MockGroup> anyGroupQueryWrapper() {
        return any(QueryWrapper.class);
    }
}
