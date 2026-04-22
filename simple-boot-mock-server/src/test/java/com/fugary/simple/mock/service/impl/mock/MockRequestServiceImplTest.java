package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockRequestServiceImplTest {

    @Mock
    private MockDataService mockDataService;

    @Mock
    private MockSchemaService mockSchemaService;

    @Mock
    private ScriptEngineProvider scriptEngineProvider;

    private MockRequestServiceImpl mockRequestService;

    @BeforeEach
    void setUp() {
        mockRequestService = spy(new MockRequestServiceImpl());
        ReflectionTestUtils.setField(mockRequestService, "mockDataService", mockDataService);
        ReflectionTestUtils.setField(mockRequestService, "mockSchemaService", mockSchemaService);
        ReflectionTestUtils.setField(mockRequestService, "scriptEngineProvider", scriptEngineProvider);
        lenient().doReturn(true).when(mockRequestService).saveOrUpdate(any(MockRequest.class));
        lenient().doAnswer(invocation -> SimpleResultUtils.createSimpleResult((MockRequest) invocation.getArgument(0)))
                .when(mockRequestService).newSaveOrUpdate(any(MockRequest.class));
        lenient().when(mockDataService.list(anyQueryWrapper())).thenReturn(Collections.emptyList());
        lenient().when(mockSchemaService.list(anySchemaQueryWrapper())).thenReturn(Collections.emptyList());
    }

    @Test
    void copyMockRequestShouldRenameWhenCopyingWithinSameScenario() {
        MockRequest source = createRequest(1, 10, "online", "User Detail");
        doReturn(source).when(mockRequestService).getById(anyInt());

        boolean result = mockRequestService.copyMockRequest(1, null, "online");

        ArgumentCaptor<MockRequest> captor = ArgumentCaptor.forClass(MockRequest.class);
        verify(mockRequestService).saveOrUpdate(captor.capture());
        MockRequest saved = captor.getValue();
        assertTrue(result);
        assertEquals("online", saved.getScenarioCode());
        assertEquals("User Detail-copy", saved.getRequestName());
    }

    @Test
    void copyMockRequestShouldKeepNameWhenCopyingToAnotherScenario() {
        MockRequest source = createRequest(1, 10, "online", "User Detail");
        doReturn(source).when(mockRequestService).getById(anyInt());

        boolean result = mockRequestService.copyMockRequest(1, null, "offline");

        ArgumentCaptor<MockRequest> captor = ArgumentCaptor.forClass(MockRequest.class);
        verify(mockRequestService).saveOrUpdate(captor.capture());
        MockRequest saved = captor.getValue();
        assertTrue(result);
        assertEquals("offline", saved.getScenarioCode());
        assertEquals("User Detail", saved.getRequestName());
    }

    @Test
    void moveMockRequestShouldUpdateScenarioWithoutRenaming() {
        MockRequest source = createRequest(1, 10, "online", "User Detail");
        doReturn(source).when(mockRequestService).getById(anyInt());

        MockRequest saved = mockRequestService.moveMockRequest(1, "offline").getResultData();

        verify(mockRequestService).newSaveOrUpdate(source);
        assertEquals("offline", saved.getScenarioCode());
        assertEquals("User Detail", saved.getRequestName());
    }

    @Test
    void moveMockRequestShouldSkipWhenTargetScenarioIsSame() {
        MockRequest source = createRequest(1, 10, "online", "User Detail");
        doReturn(source).when(mockRequestService).getById(anyInt());

        assertEquals(2000, mockRequestService.moveMockRequest(1, "online").getCode());
    }

    @Test
    void transferMockRequestsShouldCopyMultipleRequestsToTargetScenario() {
        doAnswer(invocation -> {
            Integer id = invocation.getArgument(0);
            if (id == 1) {
                return createRequest(1, 10, "online", "User Detail");
            }
            if (id == 2) {
                return createRequest(2, 10, "online", "Order Detail");
            }
            return null;
        }).when(mockRequestService).getById(anyInt());

        SimpleResult<List<MockRequest>> result = mockRequestService.transferMockRequests(List.of(1, 2),
                "copy", "offline");

        ArgumentCaptor<MockRequest> captor = ArgumentCaptor.forClass(MockRequest.class);
        verify(mockRequestService, times(2)).saveOrUpdate(captor.capture());
        List<MockRequest> savedRequests = captor.getAllValues();
        assertTrue(result.isSuccess());
        assertTrue(result.getResultData().isEmpty());
        assertEquals("offline", savedRequests.get(0).getScenarioCode());
        assertEquals("offline", savedRequests.get(1).getScenarioCode());
        assertEquals("User Detail", savedRequests.get(0).getRequestName());
        assertEquals("Order Detail", savedRequests.get(1).getRequestName());
    }

    @Test
    void transferMockRequestsShouldSkipWhenMovingToSameScenario() {
        MockRequest source = createRequest(1, 10, "online", "User Detail");
        doReturn(source).when(mockRequestService).getById(1);

        SimpleResult<List<MockRequest>> result = mockRequestService.transferMockRequests(List.of(1),
                "move", "online");

        assertEquals(2000, result.getCode());
        assertEquals(List.of(1), result.getInfos().get("skippedRequestIds"));
    }

    private MockRequest createRequest(Integer id, Integer groupId, String scenarioCode, String requestName) {
        MockRequest request = new MockRequest();
        request.setId(id);
        request.setGroupId(groupId);
        request.setScenarioCode(scenarioCode);
        request.setMethod("GET");
        request.setRequestPath("/users/{id}");
        request.setRequestName(requestName);
        return request;
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<MockData> anyQueryWrapper() {
        return any(QueryWrapper.class);
    }

    @SuppressWarnings("unchecked")
    private QueryWrapper<MockSchema> anySchemaQueryWrapper() {
        return any(QueryWrapper.class);
    }
}
