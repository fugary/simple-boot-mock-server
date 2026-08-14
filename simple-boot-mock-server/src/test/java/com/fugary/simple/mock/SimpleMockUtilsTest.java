package com.fugary.simple.mock;

import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_GROUP;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleMockUtilsTest {

    @Test
    void calcProxyUrlInfoShouldPreferRequestProxyUrl() {
        MockGroup group = new MockGroup();
        group.setProxyUrl("http://group.example.com");
        MockRequest request = new MockRequest();
        request.setProxyUrl("http://request.example.com");

        Pair<String, String> proxyUrlInfo = SimpleMockUtils.calcProxyUrlInfo(group, request);

        assertNotNull(proxyUrlInfo);
        assertEquals("http://request.example.com", proxyUrlInfo.getLeft());
        assertEquals(GROUP_REQUEST, proxyUrlInfo.getRight());
    }

    @Test
    void calcProxyUrlInfoShouldFallbackToGroupProxyUrl() {
        MockGroup group = new MockGroup();
        group.setProxyUrl("http://group.example.com");
        MockRequest request = new MockRequest();

        Pair<String, String> proxyUrlInfo = SimpleMockUtils.calcProxyUrlInfo(group, request);

        assertNotNull(proxyUrlInfo);
        assertEquals("http://group.example.com", proxyUrlInfo.getLeft());
        assertEquals(GROUP_GROUP, proxyUrlInfo.getRight());
    }

    @Test
    void calcProxyUrlInfoShouldUseEnabledProxyUrl() {
        MockGroup group = new MockGroup();
        group.setProxyUrl("[{\"enabled\":false,\"value\":\"http://disabled.example.com\"},"
                + "{\"enabled\":true,\"value\":\"http://dev.example.com\"}]");

        Pair<String, String> proxyUrlInfo = SimpleMockUtils.calcProxyUrlInfo(group, null);

        assertNotNull(proxyUrlInfo);
        assertEquals("http://dev.example.com", proxyUrlInfo.getLeft());
        assertEquals(GROUP_GROUP, proxyUrlInfo.getRight());
    }

    @Test
    void calcValidProxyUrlInfoShouldIgnoreInvalidProxyUrl() {
        MockGroup group = new MockGroup();
        group.setProxyUrl("ftp://invalid.example.com");

        assertNull(SimpleMockUtils.calcValidProxyUrlInfo(group, null));
    }

    @Test
    void formatFileSizeShouldFormatCorrectly() {
        assertEquals("0B", SimpleMockUtils.formatFileSize(0));
        assertEquals("500B", SimpleMockUtils.formatFileSize(500));
        assertEquals("1KB", SimpleMockUtils.formatFileSize(1024));
        assertEquals("1.50KB", SimpleMockUtils.formatFileSize(1536));
        assertEquals("10MB", SimpleMockUtils.formatFileSize(10 * 1024 * 1024));
        assertEquals("21.86MB", SimpleMockUtils.formatFileSize(22923828));
    }

    @Test
    void isSupportedImportFileShouldValidateCorrectly() {
        assertTrue(SimpleMockUtils.isSupportedImportFile("mock-data.json"));
        assertTrue(SimpleMockUtils.isSupportedImportFile("openapi.yaml"));
        assertTrue(SimpleMockUtils.isSupportedImportFile("swagger.YML"));
        assertTrue(SimpleMockUtils.isSupportedImportFile("network.har"));
        assertTrue(SimpleMockUtils.isSupportedImportFile("test.JSON"));

        assertFalse(SimpleMockUtils.isSupportedImportFile("data.xlsx"));
        assertFalse(SimpleMockUtils.isSupportedImportFile("report.doc"));
        assertFalse(SimpleMockUtils.isSupportedImportFile("image.png"));
        assertFalse(SimpleMockUtils.isSupportedImportFile("archive.zip"));
        assertFalse(SimpleMockUtils.isSupportedImportFile("script.sh"));
        assertFalse(SimpleMockUtils.isSupportedImportFile(""));
        assertFalse(SimpleMockUtils.isSupportedImportFile(null));
    }
}
