package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.imports.fastmock.FastMockImporterImpl;
import com.fugary.simple.mock.imports.har.HarImporterImpl;
import com.fugary.simple.mock.imports.postman.PostmanImporterImpl;
import com.fugary.simple.mock.imports.swagger.SwaggerImporterImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 导入器指纹匹配及智能诊断单元测试
 */
public class MockGroupImporterMatchTest {

    private List<MockGroupImporter> importers;

    private SimpleMockGroupImporterImpl simpleImporter;
    private SwaggerImporterImpl swaggerImporter;
    private PostmanImporterImpl postmanImporter;
    private HarImporterImpl harImporter;
    private FastMockImporterImpl fastMockImporter;

    @BeforeEach
    public void setup() {
        simpleImporter = new SimpleMockGroupImporterImpl();
        swaggerImporter = new SwaggerImporterImpl();
        postmanImporter = new PostmanImporterImpl();
        harImporter = new HarImporterImpl();
        fastMockImporter = new FastMockImporterImpl();

        importers = List.of(simpleImporter, swaggerImporter, postmanImporter, harImporter, fastMockImporter);
    }

    @Test
    public void testSimpleMatch() {
        String simpleJson = "{\"groups\":[{\"groupName\":\"test\",\"requests\":[]}]}";
        Assertions.assertTrue(simpleImporter.match(simpleJson));
        Assertions.assertFalse(swaggerImporter.match(simpleJson));
        Assertions.assertFalse(postmanImporter.match(simpleJson));
        Assertions.assertFalse(harImporter.match(simpleJson));
        Assertions.assertFalse(fastMockImporter.match(simpleJson));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, simpleJson);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("simple", detected.getType());
    }

    @Test
    public void testSwaggerJsonMatch() {
        String swaggerJson = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"Test API\",\"version\":\"1.0\"},\"paths\":{}}";
        Assertions.assertTrue(swaggerImporter.match(swaggerJson));
        Assertions.assertFalse(simpleImporter.match(swaggerJson));
        Assertions.assertFalse(postmanImporter.match(swaggerJson));
        Assertions.assertFalse(harImporter.match(swaggerJson));
        Assertions.assertFalse(fastMockImporter.match(swaggerJson));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, swaggerJson);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("swagger", detected.getType());
    }

    @Test
    public void testSwaggerYamlMatch() {
        String swaggerYaml = "openapi: 3.0.0\ninfo:\n  title: Sample API\n  version: 0.1.0\npaths:\n  /users:\n    get:\n      summary: list";
        Assertions.assertTrue(swaggerImporter.match(swaggerYaml));
        Assertions.assertFalse(simpleImporter.match(swaggerYaml));
        Assertions.assertFalse(postmanImporter.match(swaggerYaml));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, swaggerYaml);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("swagger", detected.getType());
    }

    @Test
    public void testPostmanMatch() {
        String postmanJson = "{\"info\":{\"_postman_id\":\"12345\",\"name\":\"My Collection\",\"schema\":\"https://schema.getpostman.com/json/collection/v2.1.0/collection.json\"},\"item\":[]}";
        Assertions.assertTrue(postmanImporter.match(postmanJson));
        Assertions.assertFalse(simpleImporter.match(postmanJson));
        Assertions.assertFalse(harImporter.match(postmanJson));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, postmanJson);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("postman", detected.getType());
    }

    @Test
    public void testHarMatch() {
        String harJson = "{\"log\":{\"version\":\"1.2\",\"creator\":{\"name\":\"Chrome\"},\"entries\":[]}}";
        Assertions.assertTrue(harImporter.match(harJson));
        Assertions.assertFalse(simpleImporter.match(harJson));
        Assertions.assertFalse(postmanImporter.match(harJson));
        Assertions.assertFalse(swaggerImporter.match(harJson));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, harJson);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("har", detected.getType());
    }

    @Test
    public void testFastMockMatch() {
        String fastMockJson = "[{\"id\":1,\"folderId\":0,\"url\":\"/api/test\",\"method\":\"get\",\"mockRule\":\"{}\"}]";
        Assertions.assertTrue(fastMockImporter.match(fastMockJson));
        Assertions.assertFalse(simpleImporter.match(fastMockJson));
        Assertions.assertFalse(harImporter.match(fastMockJson));
        Assertions.assertFalse(swaggerImporter.match(fastMockJson));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, fastMockJson);
        Assertions.assertNotNull(detected);
        Assertions.assertEquals("fastmock", detected.getType());
    }

    @Test
    public void testInvalidOrEmptyData() {
        Assertions.assertFalse(simpleImporter.match(null));
        Assertions.assertFalse(simpleImporter.match(""));
        Assertions.assertFalse(simpleImporter.match("invalid json content"));

        MockGroupImporter detected = MockGroupImporter.detectImporter(importers, "some random text");
        Assertions.assertNull(detected);
    }

    @Test
    public void testIsSupportAndFindImporter() {
        Assertions.assertTrue(simpleImporter.isSupport("simple"));
        Assertions.assertTrue(simpleImporter.isSupport("SIMPLE"));
        Assertions.assertFalse(simpleImporter.isSupport("swagger"));
        Assertions.assertFalse(simpleImporter.isSupport(null));

        MockGroupImporter found = MockGroupImporter.findImporter(importers, "swagger");
        Assertions.assertNotNull(found);
        Assertions.assertEquals("swagger", found.getType());

        MockGroupImporter notFound = MockGroupImporter.findImporter(importers, "unknown_type");
        Assertions.assertNull(notFound);

        Assertions.assertEquals("simple", simpleImporter.getTypeName()); // When messageSource is null in unit test, falls back to type
        Assertions.assertEquals("swagger", MockGroupImporter.getTypeName(importers, "swagger"));
        Assertions.assertEquals("custom", MockGroupImporter.getTypeName(importers, "custom"));
    }
}
