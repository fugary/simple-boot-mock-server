package com.fugary.simple.mock.service.impl.mock;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.imports.SimpleMockGroupImporterImpl;
import com.fugary.simple.mock.imports.postman.PostmanImporterImpl;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.query.MockGroupImportParamVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockGroupImportStreamTest {
    private final List<TrackingStream> streams = new ArrayList<>();

    @AfterEach
    void resetMessages() {
        new SimpleResultUtils().injectMessageSource(null);
    }

    private MockGroupServiceImpl service() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        new SimpleResultUtils().injectMessageSource(messages);
        MockGroupServiceImpl service = new MockGroupServiceImpl() {
            @Override
            protected List<MockGroup> checkGroupsExists(List<? extends MockGroup> groups) {
                return Collections.emptyList();
            }
        };
        ReflectionTestUtils.setField(service, "mockGroupImporters", List.of(new SimpleMockGroupImporterImpl(), new PostmanImporterImpl()));
        return service;
    }

    private MultipartFile file(String json) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("sample.json");
        when(file.getInputStream()).thenAnswer(invocation -> {
            TrackingStream stream = new TrackingStream(json.getBytes(StandardCharsets.UTF_8));
            streams.add(stream);
            return stream;
        });
        return file;
    }

    @Test
    void successfulNativeImportReadsOnceAndClosesStream() throws Exception {
        MockGroupImportParamVo params = new MockGroupImportParamVo();
        params.setType("simple");
        assertTrue(service().toImportGroups(List.of(file("{\"groups\":[{\"groupName\":\"A\",\"groupPath\":\"a\",\"requests\":[]}]}")), params).isSuccess());
        assertEquals(1, streams.size());
        assertTrue(streams.get(0).closed);
    }

    @Test
    void wrongFormatReopensForDiagnosisAndClosesBothStreams() throws Exception {
        MockGroupImportParamVo params = new MockGroupImportParamVo();
        params.setType("simple");
        var result = service().toImportGroups(List.of(file("{\"info\":{\"_postman_id\":\"demo\"},\"item\":[]}")), params);
        assertEquals(MockErrorConstants.CODE_2003, result.getCode());
        assertTrue(result.getMessage().contains("Postman"));
        assertEquals(2, streams.size());
        assertTrue(streams.stream().allMatch(stream -> stream.closed));
    }

    private static class TrackingStream extends ByteArrayInputStream {
        private boolean closed;
        TrackingStream(byte[] data) { super(data); }
        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
