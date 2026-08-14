package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.imports.har.HarImporterImpl;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * HAR 导入器单元测试
 *
 * @author gary.fu
 */
public class HarImporterTest {

    private HarImporterImpl harImporter;

    @BeforeEach
    public void setUp() {
        harImporter = new HarImporterImpl();
    }

    @Test
    public void testIsSupport() {
        Assertions.assertTrue(harImporter.isSupport("har"));
        Assertions.assertTrue(harImporter.isSupport("HAR"));
        Assertions.assertFalse(harImporter.isSupport("postman"));
        Assertions.assertFalse(harImporter.isSupport("swagger"));
    }

    @Test
    public void testImportStandardHar() {
        String base64Body = Base64.getEncoder().encodeToString("{\"message\":\"Hello Base64\"}".getBytes(StandardCharsets.UTF_8));

        String harJson = "{\n" +
                "  \"log\": {\n" +
                "    \"version\": \"1.2\",\n" +
                "    \"creator\": { \"name\": \"Chrome\", \"version\": \"120.0\" },\n" +
                "    \"entries\": [\n" +
                "      {\n" +
                "        \"startedDateTime\": \"2024-03-20T10:00:00.000Z\",\n" +
                "        \"time\": 45.5,\n" +
                "        \"request\": {\n" +
                "          \"method\": \"GET\",\n" +
                "          \"url\": \"https://api.example.com/v1/users?page=1&size=20\",\n" +
                "          \"httpVersion\": \"HTTP/1.1\",\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \":method\", \"value\": \"GET\" },\n" +
                "            { \"name\": \"Authorization\", \"value\": \"Bearer token123\" },\n" +
                "            { \"name\": \"Accept\", \"value\": \"application/json\" }\n" +
                "          ],\n" +
                "          \"queryString\": [\n" +
                "            { \"name\": \"page\", \"value\": \"1\" },\n" +
                "            { \"name\": \"size\", \"value\": \"20\" }\n" +
                "          ],\n" +
                "          \"headersSize\": -1,\n" +
                "          \"bodySize\": -1\n" +
                "        },\n" +
                "        \"response\": {\n" +
                "          \"status\": 200,\n" +
                "          \"statusText\": \"OK\",\n" +
                "          \"httpVersion\": \"HTTP/1.1\",\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \"Content-Type\", \"value\": \"application/json; charset=utf-8\" },\n" +
                "            { \"name\": \"X-Custom-Header\", \"value\": \"test-val\" }\n" +
                "          ],\n" +
                "          \"content\": {\n" +
                "            \"size\": 50,\n" +
                "            \"mimeType\": \"application/json\",\n" +
                "            \"text\": \"{\\\"code\\\":0,\\\"data\\\":[{\\\"id\\\":1,\\\"name\\\":\\\"Alice\\\"}]}\"\n" +
                "          },\n" +
                "          \"headersSize\": -1,\n" +
                "          \"bodySize\": 50\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"startedDateTime\": \"2024-03-20T10:01:00.000Z\",\n" +
                "        \"time\": 80,\n" +
                "        \"request\": {\n" +
                "          \"method\": \"POST\",\n" +
                "          \"url\": \"https://api.example.com/v1/users\",\n" +
                "          \"httpVersion\": \"HTTP/1.1\",\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \"Content-Type\", \"value\": \"application/json\" }\n" +
                "          ],\n" +
                "          \"queryString\": [],\n" +
                "          \"postData\": {\n" +
                "            \"mimeType\": \"application/json\",\n" +
                "            \"text\": \"{\\\"name\\\":\\\"Bob\\\",\\\"age\\\":25}\"\n" +
                "          },\n" +
                "          \"headersSize\": -1,\n" +
                "          \"bodySize\": -1\n" +
                "        },\n" +
                "        \"response\": {\n" +
                "          \"status\": 201,\n" +
                "          \"statusText\": \"Created\",\n" +
                "          \"httpVersion\": \"HTTP/1.1\",\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \"Content-Type\", \"value\": \"application/json\" }\n" +
                "          ],\n" +
                "          \"content\": {\n" +
                "            \"size\": 30,\n" +
                "            \"mimeType\": \"application/json\",\n" +
                "            \"text\": \"" + base64Body + "\",\n" +
                "            \"encoding\": \"base64\"\n" +
                "          },\n" +
                "          \"headersSize\": -1,\n" +
                "          \"bodySize\": 30\n" +
                "        }\n" +
                "      },\n" +
                "      {\n" +
                "        \"startedDateTime\": \"2024-03-20T10:02:00.000Z\",\n" +
                "        \"time\": 10,\n" +
                "        \"request\": {\n" +
                "          \"method\": \"GET\",\n" +
                "          \"url\": \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==\",\n" +
                "          \"httpVersion\": \"HTTP/1.1\",\n" +
                "          \"headers\": [],\n" +
                "          \"queryString\": [],\n" +
                "          \"headersSize\": -1,\n" +
                "          \"bodySize\": -1\n" +
                "        },\n" +
                "        \"response\": {\n" +
                "          \"status\": 200,\n" +
                "          \"statusText\": \"OK\",\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \"Content-Type\", \"value\": \"image/png\" }\n" +
                "          ],\n" +
                "          \"content\": {\n" +
                "            \"size\": 1000,\n" +
                "            \"mimeType\": \"image/png\",\n" +
                "            \"text\": \"\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        ExportMockVo exportMockVo = harImporter.doImport(harJson);
        Assertions.assertNotNull(exportMockVo);
        Assertions.assertNotNull(exportMockVo.getGroups());
        Assertions.assertEquals(1, exportMockVo.getGroups().size());

        ExportGroupVo group = exportMockVo.getGroups().get(0);
        Assertions.assertEquals("HAR-api.example.com", group.getGroupName());
        List<ExportRequestVo> requests = group.getRequests();
        Assertions.assertNotNull(requests);
        // data: 伪协议应被过滤掉，只保留 2 个 API 请求
        Assertions.assertEquals(2, requests.size());

        // 验证第 1 个请求 GET /v1/users
        ExportRequestVo req1 = requests.get(0);
        Assertions.assertEquals("GET", req1.getMethod());
        Assertions.assertEquals("/v1/users", req1.getRequestPath());
        Assertions.assertNull(req1.getRequestName());
        Assertions.assertNull(req1.getDelay());
        Assertions.assertNotNull(req1.getMockParams());
        Assertions.assertTrue(req1.getMockParams().contains("page"));
        Assertions.assertTrue(req1.getMockParams().contains("token123"));
        Assertions.assertFalse(req1.getMockParams().contains(":method")); // 过滤伪头

        List<ExportDataVo> dataList1 = req1.getDataList();
        Assertions.assertEquals(1, dataList1.size());
        ExportDataVo data1 = dataList1.get(0);
        Assertions.assertEquals(200, data1.getStatusCode());
        Assertions.assertTrue(data1.getResponseBody().contains("Alice"));
        Assertions.assertEquals("json", data1.getResponseFormat());

        // 验证第 2 个请求 POST /v1/users (Base64 decode)
        ExportRequestVo req2 = requests.get(1);
        Assertions.assertEquals("POST", req2.getMethod());
        Assertions.assertEquals("/v1/users", req2.getRequestPath());
        Assertions.assertTrue(req2.getMockParams().contains("Bob"));

        List<ExportDataVo> dataList2 = req2.getDataList();
        Assertions.assertEquals(1, dataList2.size());
        ExportDataVo data2 = dataList2.get(0);
        Assertions.assertEquals(201, data2.getStatusCode());
        Assertions.assertEquals("{\"message\":\"Hello Base64\"}", data2.getResponseBody());
    }

    @Test
    public void testEmptyOrInvalidHar() {
        Assertions.assertNull(harImporter.doImport(null));
        Assertions.assertNull(harImporter.doImport(""));
        Assertions.assertNull(harImporter.doImport("{\"log\":{\"entries\":[]}}"));
        Assertions.assertNull(harImporter.doImport("invalid json"));
    }

    @Test
    public void testHeadersFilterAndLengthLimit() {
        String longHeaderVal = "a".repeat(1000);
        String harJson = "{\n" +
                "  \"log\": {\n" +
                "    \"version\": \"1.2\",\n" +
                "    \"creator\": { \"name\": \"Chrome\", \"version\": \"120.0\" },\n" +
                "    \"entries\": [\n" +
                "      {\n" +
                "        \"request\": {\n" +
                "          \"method\": \"GET\",\n" +
                "          \"url\": \"https://api.example.com/test/headers\"\n" +
                "        },\n" +
                "        \"response\": {\n" +
                "          \"status\": 200,\n" +
                "          \"headers\": [\n" +
                "            { \"name\": \"Content-Security-Policy\", \"value\": \"default-src 'self'; script-src 'unsafe-inline'\" },\n" +
                "            { \"name\": \"Access-Control-Expose-Headers\", \"value\": \"*\" },\n" +
                "            { \"name\": \"Strict-Transport-Security\", \"value\": \"max-age=31536000\" },\n" +
                "            { \"name\": \"X-Business-Header-1\", \"value\": \"" + longHeaderVal + "\" },\n" +
                "            { \"name\": \"X-Business-Header-2\", \"value\": \"" + longHeaderVal + "\" }\n" +
                "          ],\n" +
                "          \"content\": {\n" +
                "            \"mimeType\": \"application/json\",\n" +
                "            \"text\": \"{}\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        ExportMockVo exportMockVo = harImporter.doImport(harJson);
        Assertions.assertNotNull(exportMockVo);
        ExportDataVo dataVo = exportMockVo.getGroups().get(0).getRequests().get(0).getDataList().get(0);
        Assertions.assertNotNull(dataVo.getHeaders());
        // 验证系统安全与 CORS 响应头已被过滤
        Assertions.assertFalse(dataVo.getHeaders().contains("Content-Security-Policy"));
        Assertions.assertFalse(dataVo.getHeaders().contains("Access-Control-Expose-Headers"));
        Assertions.assertFalse(dataVo.getHeaders().contains("Strict-Transport-Security"));
        // 验证安全长度截断保护（严格不超过 1800 字符）
        Assertions.assertTrue(dataVo.getHeaders().length() <= HarImporterImpl.MAX_SAFE_HEADERS_LENGTH);
    }
}
