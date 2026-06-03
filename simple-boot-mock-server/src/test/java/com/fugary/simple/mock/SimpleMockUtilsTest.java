package com.fugary.simple.mock;

import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_GROUP;
import static com.fugary.simple.mock.contants.MockDiagnoseConstants.GROUP_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
