package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.config.ApplicationConfig;
import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.web.controllers.admin.MockImportConfigController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class LargeImportTest {
    private CommonsMultipartResolver resolver(SimpleMockConfigProperties properties) {
        ApplicationConfig config = new ApplicationConfig();
        ReflectionTestUtils.setField(config, "simpleMockConfigProperties", properties);
        return config.multipartResolver();
    }

    private MockHttpServletRequest request(int... sizes) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int i = 0; i < sizes.length; i++) {
            body.write(("--boundary\r\nContent-Disposition: form-data; name=\"files\"; filename=\"data" + i
                    + ".json\"\r\nContent-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(new byte[sizes[i]]);
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write("--boundary--\r\n".getBytes(StandardCharsets.UTF_8));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("multipart/form-data; boundary=boundary");
        request.setContent(body.toByteArray());
        return request;
    }

    @Test
    void acceptsLargeDiskBackedUploadAndMultipleFiles() throws Exception {
        CommonsMultipartResolver resolver = resolver(new SimpleMockConfigProperties());
        MultipartHttpServletRequest request = resolver.resolveMultipart(request(11 * 1024 * 1024));
        try {
            CommonsMultipartFile file = (CommonsMultipartFile) request.getFile("files");
            assertEquals(11 * 1024 * 1024, file.getSize());
            assertFalse(file.getFileItem().isInMemory());
        } finally {
            resolver.cleanupMultipart(request);
        }
        request = resolver.resolveMultipart(request(40 * 1024 * 1024, 50 * 1024 * 1024));
        try {
            assertEquals(2, request.getFiles("files").size());
        } finally {
            resolver.cleanupMultipart(request);
        }
    }

    @Test
    void enforcesConfiguredPerFileAndAggregateLimits() throws Exception {
        SimpleMockConfigProperties properties = new SimpleMockConfigProperties();
        properties.setMaxUploadSize(4096);
        properties.setMaxUploadFileSize(1024);
        CommonsMultipartResolver perFile = resolver(properties);
        assertThrows(MaxUploadSizeExceededException.class, () -> perFile.resolveMultipart(request(1025)));
        properties.setMaxUploadFileSize(4096);
        CommonsMultipartResolver aggregate = resolver(properties);
        assertThrows(MaxUploadSizeExceededException.class, () -> aggregate.resolveMultipart(request(2048, 2048)));
        assertEquals(4096L, new MockImportConfigController(properties).importConfig().getResultData().get("maxRequestSize"));
        assertEquals(4096L, new MockImportConfigController(properties).importConfig().getResultData().get("maxFileSize"));
    }

    @Test
    void nativeImportStreamsPayloadLargerThanTenMb() throws Exception {
        String value = "x".repeat(11 * 1024 * 1024);
        String json = "{\"groups\":[{\"groupName\":\"large\",\"groupPath\":\"large\",\"requests\":[],\"description\":\"" + value + "\"}]}";
        SimpleMockGroupImporterImpl importer = new SimpleMockGroupImporterImpl();
        assertEquals(value, importer.doImportStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .getGroups().get(0).getDescription());
        assertNull(importer.doImportStream(new ByteArrayInputStream("invalid".getBytes(StandardCharsets.UTF_8))));
        assertNull(importer.doImportStream(new ByteArrayInputStream("{\"info\":{}}".getBytes(StandardCharsets.UTF_8))));
    }
}
