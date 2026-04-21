package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
        doReturn(true).when(mockRequestService).saveOrUpdate(any(MockRequest.class));
        when(mockDataService.list(anyQueryWrapper())).thenReturn(Collections.emptyList());
        when(mockSchemaService.list(anySchemaQueryWrapper())).thenReturn(Collections.emptyList());
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
