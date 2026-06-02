package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.push.MockPostScriptProcessor;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        lenient().when(scriptEngineProvider.mock(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mockPostScriptProcessor.process(any(MockRequest.class), any(MockData.class), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        lenient().when(mockRequestService.matchRequestPattern(anyString())).thenReturn(true);
        lenient().when(mockRequestService.findForceMockData(anyList(), any())).thenAnswer(invocation -> {
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

    @Test
    void matchMockDataShouldRecordForcedRequestDiagnoseWithoutBlankScenario() {
        MockGroup mockGroup = createGroup("demo", null);
        MockRequest request = createRequest(31, mockGroup.getId(), "/users/{id}", null);
        MockData data = createData(301, mockGroup.getId(), request.getId(), "{\"ok\":true}");
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>(List.of(request)));
        when(mockRequestService.loadAllDataByRequest(request.getId())).thenReturn(List.of(data));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                request.getId(),
                0,
                group -> true,
                diagnose
        );

        assertNotNull(result.getMiddle());
        assertEquals(request.getId(), diagnose.getRequest().getId());
        assertNull(diagnose.getScenario());
        assertTrue(diagnose.getSteps().stream().anyMatch(step -> "force_request_selected".equals(step.getCode())));
        assertTrue(diagnose.getSteps().stream().noneMatch(step -> "scenario".equals(step.getStage())));
    }

    @Test
    void matchMockDataShouldRecordDataCandidatesBeforeForcedDataDiagnose() {
        MockGroup mockGroup = createGroup("demo", null);
        MockRequest request = createRequest(33, mockGroup.getId(), "/users/{id}", null);
        MockData data = createData(303, mockGroup.getId(), request.getId(), "{\"ok\":true}");
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>(List.of(request)));
        when(mockRequestService.loadAllDataByRequest(request.getId())).thenReturn(List.of(data));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                request.getId(),
                data.getId(),
                group -> true,
                diagnose
        );

        assertNotNull(result.getRight());
        assertEquals(data.getId(), diagnose.getData().getId());
        int dataCandidatesIndex = indexOfStepCode(diagnose, "data_candidates_loaded");
        int forceDataIndex = indexOfStepCode(diagnose, "force_data_selected");
        assertTrue(dataCandidatesIndex >= 0);
        assertTrue(forceDataIndex >= 0);
        assertTrue(dataCandidatesIndex < forceDataIndex);
    }

    @Test
    void matchMockDataShouldRecordDataSelectionModeDiagnose() {
        MockGroup mockGroup = createGroup("demo", null);
        MockRequest request = createRequest(34, mockGroup.getId(), "/users/{id}", null);
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_ROUND_ROBIN);
        MockData firstData = createData(304, mockGroup.getId(), request.getId(), "{\"index\":1}");
        MockData secondData = createData(305, mockGroup.getId(), request.getId(), "{\"index\":2}");
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>(List.of(request)));
        when(mockRequestService.loadAllDataByRequest(request.getId())).thenReturn(List.of(firstData, secondData));
        when(mockRequestService.findMockData(any(MockRequest.class), anyList())).thenReturn(secondData);

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                0,
                0,
                group -> true,
                diagnose
        );

        assertEquals(secondData.getId(), result.getRight().getId());
        MockDiagnoseVo.DataItem dataItem = (MockDiagnoseVo.DataItem) diagnose.getData();
        assertEquals("round_robin", dataItem.getDataSelection());
        MockDiagnoseVo.Step selectionStep = diagnose.getSteps().stream()
                .filter(step -> "default_data_selected".equals(step.getCode()))
                .findFirst().orElse(null);
        assertNotNull(selectionStep);
        assertEquals("round_robin", ((Map<?, ?>) selectionStep.getDetails().get("data"))
                .get("dataSelection"));
    }

    @Test
    void matchMockDataShouldDiagnoseDefaultScenarioWhenScenariosExist() {
        MockGroup mockGroup = createGroup("demo", null);
        MockRequest request = createRequest(32, mockGroup.getId(), "/users/{id}", null);
        MockData data = createData(302, mockGroup.getId(), request.getId(), "{\"ok\":true}");
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockScenarioService.count(any(QueryWrapper.class))).thenReturn(1L);
        when(mockRequestService.list(any(QueryWrapper.class))).thenReturn(new ArrayList<>(List.of(request)));
        when(mockRequestService.loadAllDataByRequest(request.getId())).thenReturn(List.of(data));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                0,
                0,
                group -> true,
                diagnose
        );

        assertNotNull(result.getMiddle());
        assertNotNull(diagnose.getScenario());
        assertEquals(Boolean.TRUE, diagnose.getScenario().getDefaultScenario());
        assertTrue(diagnose.getSteps().stream().anyMatch(step -> "scenario_selected".equals(step.getCode())));
    }

    @Test
    void matchMockDataShouldDiagnoseDisabledGroup() {
        MockGroup disabledGroup = createGroup("demo", null);
        disabledGroup.setStatus(0);
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(disabledGroup).when(mockGroupService).getOne(any());

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                0,
                0,
                group -> true,
                diagnose
        );

        assertNull(result.getLeft());
        assertEquals(disabledGroup.getId(), diagnose.getGroup().getId());
        assertTrue(diagnose.getSteps().stream().anyMatch(step -> "group_disabled".equals(step.getCode())));
        assertTrue(diagnose.getSteps().stream().noneMatch(step -> "group_not_found".equals(step.getCode())));
    }

    @Test
    void matchMockDataShouldDiagnoseDisabledRequest() {
        MockGroup mockGroup = createGroup("demo", null);
        MockRequest disabledRequest = createRequest(41, mockGroup.getId(), "/users/{id}", null);
        disabledRequest.setStatus(0);
        MockDiagnoseVo diagnose = new MockDiagnoseVo();

        doReturn(mockGroup).when(mockGroupService).getOne(any());
        when(mockRequestService.list(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(disabledRequest)));

        Triple<MockGroup, MockRequest, MockData> result = mockGroupService.matchMockData(
                buildRequest("/mock/demo/users/1"),
                0,
                0,
                group -> true,
                diagnose
        );

        assertNull(result.getMiddle());
        assertEquals(disabledRequest.getId(), diagnose.getRequest().getId());
        assertTrue(diagnose.getSteps().stream().anyMatch(step -> "request_disabled".equals(step.getCode())));
        assertTrue(diagnose.getSteps().stream().noneMatch(step -> "request_path_not_matched".equals(step.getCode())));
    }

    private MockHttpServletRequest buildRequest(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath(servletPath);
        return request;
    }

    private int indexOfStepCode(MockDiagnoseVo diagnose, String code) {
        for (int i = 0; i < diagnose.getSteps().size(); i++) {
            if (code.equals(diagnose.getSteps().get(i).getCode())) {
                return i;
            }
        }
        return -1;
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
