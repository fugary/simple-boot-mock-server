package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.push.MockPostScriptProcessor;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockGroupServiceImplTest {

    @Mock
    private MockRequestService mockRequestService;

    @Mock
    private MockDataService mockDataService;

    @Mock
    private MockSchemaService mockSchemaService;

    @Mock
    private ScriptEngineProvider scriptEngineProvider;

    @Mock
    private MockPostScriptProcessor mockPostScriptProcessor;

    @Mock
    private MockScenarioService mockScenarioService;

    private MockGroupServiceImpl mockGroupService;

    @BeforeEach
    void setUp() throws Exception {
        mockGroupService = spy(new MockGroupServiceImpl());
        ReflectionTestUtils.setField(mockGroupService, "mockRequestService", mockRequestService);
        ReflectionTestUtils.setField(mockGroupService, "mockDataService", mockDataService);
        ReflectionTestUtils.setField(mockGroupService, "mockSchemaService", mockSchemaService);
        ReflectionTestUtils.setField(mockGroupService, "scriptEngineProvider", scriptEngineProvider);
        ReflectionTestUtils.setField(mockGroupService, "mockPostScriptProcessor", mockPostScriptProcessor);
        ReflectionTestUtils.setField(mockGroupService, "mockScenarioService", mockScenarioService);
        mockGroupService.afterPropertiesSet();

        when(scriptEngineProvider.mock(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockPostScriptProcessor.process(any(MockRequest.class), any(MockData.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(mockRequestService.matchRequestPattern(anyString())).thenReturn(true);
        when(mockRequestService.findForceMockData(anyList(), any())).thenAnswer(invocation -> {
            List<MockData> dataList = invocation.getArgument(0);
            Integer dataId = invocation.getArgument(1);
            if (dataId == null || dataId == 0) {
                return null;
            }
            return dataList.stream().filter(item -> dataId.equals(item.getId())).findFirst().orElse(null);
        });
        lenient().when(mockRequestService.findMockDataByRequest(anyList(), any())).thenReturn(null);
        lenient().when(mockRequestService.findMockData(any(MockRequest.class), anyList())).thenAnswer(invocation -> {
            List<MockData> dataList = invocation.getArgument(1);
            return dataList.isEmpty() ? null : dataList.get(0);
        });
    }

    @Test
    void matchMockDataShouldIgnoreActiveScenarioWhenPreviewingSpecificRequest() {
        MockGroup mockGroup = createGroup("demo", "active");
        MockRequest inactiveRequest = createRequest(12, mockGroup.getId(), "/users/{id}", "inactive");
        MockData inactiveData = createData(102, mockGroup.getId(), inactiveRequest.getId(), "{\"scene\":\"inactive\"}");

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class))).thenAnswer(invocation -> {
            QueryWrapper<MockRequest> queryWrapper = invocation.getArgument(0);
            if (queryWrapper.getParamNameValuePairs().containsValue(mockGroup.getActiveScenarioCode())) {
                return new ArrayList<>();
            }
            return new ArrayList<>(List.of(inactiveRequest));
        });
        when(mockRequestService.loadAllDataByRequest(inactiveRequest.getId())).thenReturn(List.of(inactiveData));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                inactiveRequest.getId(),
                inactiveData.getId(),
                group -> true
        );

        assertNotNull(result.getMiddle());
        assertNotNull(result.getRight());
        assertEquals(inactiveRequest.getId(), result.getMiddle().getId());
        assertEquals(inactiveData.getId(), result.getRight().getId());
    }

    @Test
    void matchMockDataShouldKeepSearchingUntilSpecifiedDataIsFound() {
        MockGroup mockGroup = createGroup("demo", "active");
        MockRequest activeRequest = createRequest(21, mockGroup.getId(), "/users/{id}", "active");
        MockRequest inactiveRequest = createRequest(22, mockGroup.getId(), "/users/{id}", "inactive");
        MockData activeData = createData(201, mockGroup.getId(), activeRequest.getId(), "{\"scene\":\"active\"}");
        MockData inactiveData = createData(202, mockGroup.getId(), inactiveRequest.getId(), "{\"scene\":\"inactive\"}");

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class))).thenAnswer(invocation -> {
            QueryWrapper<MockRequest> queryWrapper = invocation.getArgument(0);
            if (queryWrapper.getParamNameValuePairs().containsValue(mockGroup.getActiveScenarioCode())) {
                return new ArrayList<>(List.of(activeRequest));
            }
            return new ArrayList<>(List.of(activeRequest, inactiveRequest));
        });
        when(mockRequestService.loadAllDataByRequest(activeRequest.getId())).thenReturn(List.of(activeData));
        when(mockRequestService.loadAllDataByRequest(inactiveRequest.getId())).thenReturn(List.of(inactiveData));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                0,
                inactiveData.getId(),
                group -> true
        );

        assertNotNull(result.getMiddle());
        assertNotNull(result.getRight());
        assertEquals(inactiveRequest.getId(), result.getMiddle().getId());
        assertEquals(inactiveData.getId(), result.getRight().getId());
    }

    private MockHttpServletRequest buildRequest(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath(servletPath);
        return request;
    }

    private MockGroup createGroup(String groupPath, String activeScenarioCode) {
        MockGroup mockGroup = new MockGroup();
        mockGroup.setId(1);
        mockGroup.setStatus(1);
        mockGroup.setGroupPath(groupPath);
        mockGroup.setActiveScenarioCode(activeScenarioCode);
        return mockGroup;
    }

    private MockRequest createRequest(Integer id, Integer groupId, String requestPath, String scenarioCode) {
        MockRequest mockRequest = new MockRequest();
        mockRequest.setId(id);
        mockRequest.setGroupId(groupId);
        mockRequest.setStatus(1);
        mockRequest.setMethod("GET");
        mockRequest.setRequestPath(requestPath);
        mockRequest.setMatchPattern("match");
        mockRequest.setScenarioCode(scenarioCode);
        return mockRequest;
    }

    private MockData createData(Integer id, Integer groupId, Integer requestId, String responseBody) {
        MockData mockData = new MockData();
        mockData.setId(id);
        mockData.setGroupId(groupId);
        mockData.setRequestId(requestId);
        mockData.setStatus(1);
        mockData.setStatusCode(200);
        mockData.setContentType("application/json");
        mockData.setResponseBody(responseBody);
        return mockData;
    }
}
