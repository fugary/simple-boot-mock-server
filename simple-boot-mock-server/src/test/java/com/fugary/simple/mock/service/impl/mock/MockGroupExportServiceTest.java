package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fugary.simple.mock.entity.mock.*;
import com.fugary.simple.mock.mapper.mock.*;
import com.fugary.simple.mock.service.mock.MockGroupExportService;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockGroupExportServiceTest {
    @TempDir Path temp;
    private DriverManagerDataSource source;
    private Connection keepAlive;
    private MockRequestServiceImpl requests;
    private MockDataServiceImpl data;
    private MockSchemaServiceImpl schemas;
    private MockScenarioServiceImpl scenarios;
    private MockGroupExportService exporter;
    private MockGroup group;

    @BeforeEach
    void setup() throws Exception {
        source = new DriverManagerDataSource("jdbc:h2:file:" + temp.resolve("export").toString()
                .replace('\\', '/') + ";CACHE_SIZE=4096;DB_CLOSE_ON_EXIT=FALSE;MAX_COMPACT_TIME=0", "sa", "");
        // Production uses a connection pool: keep the file database open between queries.
        keepAlive = source.getConnection();
        Flyway.configure().dataSource(source).locations("classpath:db/h2/migration").load().migrate();
        MybatisConfiguration config = new MybatisConfiguration();
        config.setLogImpl(NoLoggingImpl.class);
        GlobalConfig global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setTablePrefix("t_"));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(config);
        factory.setGlobalConfig(global);
        factory.setPlugins(interceptor);
        org.apache.ibatis.session.SqlSessionFactory sessions = factory.getObject();
        for (Class<?> mapper : List.of(MockRequestMapper.class, MockDataMapper.class,
                MockSchemaMapper.class, MockScenarioMapper.class)) {
            sessions.getConfiguration().addMapper(mapper);
        }
        SqlSessionTemplate session = new SqlSessionTemplate(sessions);
        requests = new MockRequestServiceImpl();
        data = new MockDataServiceImpl();
        schemas = new MockSchemaServiceImpl();
        scenarios = new MockScenarioServiceImpl();
        ReflectionTestUtils.setField(requests, "baseMapper", session.getMapper(MockRequestMapper.class));
        ReflectionTestUtils.setField(data, "baseMapper", session.getMapper(MockDataMapper.class));
        ReflectionTestUtils.setField(schemas, "baseMapper", session.getMapper(MockSchemaMapper.class));
        ReflectionTestUtils.setField(scenarios, "baseMapper", session.getMapper(MockScenarioMapper.class));
        exporter = new MockGroupExportService(requests, data, schemas, scenarios);
        group = new MockGroup();
        group.setId(1001);
        group.setGroupName("导出测试");
        group.setActiveScenarioCode("success");
        group.setCreateDate(new Date(1700000000000L));
    }

    @AfterEach
    void closeDatabase() throws Exception {
        if (keepAlive != null) keepAlive.close();
    }

    @Test
    void matchesExistingFormatIncludingSchemasScenariosAndHistoryFiltering() throws Exception {
        MockRequest request = addRequest(1001, null);
        request.setScenarioCode("success");
        requests.updateById(request);
        addRequest(1002, 1001);
        MockData item = new MockData();
        item.setId(1001);
        item.setGroupId(group.getId());
        item.setRequestId(request.getId());
        item.setResponseBody("中文\n\"quoted\"\t\\slash");
        data.save(item);
        MockData history = new MockData();
        history.setId(1002);
        history.setGroupId(group.getId());
        history.setRequestId(request.getId());
        history.setModifyFrom(item.getId());
        history.setResponseBody("old body");
        data.save(history);
        for (int level = 0; level < 3; level++) {
            MockSchema schema = new MockSchema();
            schema.setGroupId(group.getId());
            schema.setRequestId(level > 0 ? request.getId() : null);
            schema.setDataId(level > 1 ? item.getId() : null);
            schema.setResponseBodySchema("{\"type\":\"string\"}");
            schemas.save(schema);
        }
        MockScenario scenario = new MockScenario();
        scenario.setGroupId(group.getId());
        scenario.setScenarioCode("success");
        scenario.setScenarioName("成功");
        scenarios.save(scenario);
        MockGroupServiceImpl legacy = new MockGroupServiceImpl();
        ReflectionTestUtils.setField(legacy, "mockRequestService", requests);
        ReflectionTestUtils.setField(legacy, "mockDataService", data);
        ReflectionTestUtils.setField(legacy, "mockSchemaService", schemas);
        ReflectionTestUtils.setField(legacy, "mockScenarioService", scenarios);
        ExportMockVo expected = new ExportMockVo();
        expected.setGroups(legacy.loadExportGroups(List.of(group)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        exporter.writeGroups(List.of(group), output);
        assertEquals(JsonUtils.getMapper().readTree(JsonUtils.toJson(expected)),
                JsonUtils.getMapper().readTree(output.toByteArray()));
        output.reset();
        exporter.writeGroups(List.of(), output);
        assertEquals("{\"groups\":[]}", output.toString("UTF-8"));
    }

    @Test
    void propagatesDisconnectedOutputAndLeavesDatabaseUsable() throws Exception {
        addRequest(1001, null);
        assertThrows(IOException.class, () -> exporter.writeGroups(List.of(group), new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("disconnected"); }
        }));
        assertEquals(1, requests.count());
    }

    @Test
    @EnabledIfSystemProperty(named = "mock.export.large-test", matches = "true")
    void exportsFortyAndFiftyMiBFromRealDatabaseWithBoundedHeap() throws Exception {
        System.out.println("EXPORT_PROCESS pid=" + ProcessHandle.current().pid());
        addRequest(1001, null);
        // Build existing large VARCHAR rows in a separate JVM; measure only the export JVM heap.
        keepAlive.close();
        keepAlive = null;
        Process fixture = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx1g", "-cp", System.getProperty("java.class.path"), LargeFixture.class.getName(), source.getUrl())
                .redirectErrorStream(true).redirectOutput(temp.resolve("fixture.log").toFile()).start();
        try {
            assertTrue(fixture.waitFor(2, java.util.concurrent.TimeUnit.MINUTES), "large fixture preparation timed out");
            assertEquals(0, fixture.exitValue(), () -> "large database fixture preparation failed; see "
                    + temp.resolve("fixture.log"));
        } finally {
            if (fixture.isAlive()) {
                fixture.destroyForcibly();
                fixture.waitFor();
            }
        }
        System.out.println("EXPORT_FIXTURE preparationHeap=1073741824");
        keepAlive = source.getConnection();
        Path output = temp.resolve("large-export.json");
        for (java.lang.management.MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            pool.resetPeakUsage();
        }
        try (OutputStream stream = Files.newOutputStream(output)) {
            exporter.writeGroups(List.of(group), stream);
        }
        System.out.println("EXPORT_LARGE bytes=" + Files.size(output) + " maxHeap=" + Runtime.getRuntime().maxMemory());
        for (java.lang.management.MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            System.out.println("EXPORT_MEMORY " + pool.getName() + " peakUsed=" + pool.getPeakUsage().getUsed());
        }
        int bodies = 0;
        try (JsonParser parser = JsonUtils.getMapper().getFactory().createParser(output.toFile())) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "responseBody".equals(parser.currentName())) {
                    parser.nextToken();
                    // nextToken skips the large string without materializing it.
                    bodies++;
                }
            }
        }
        assertEquals(2, bodies);
        java.util.List<Long> bodyLengths = new java.util.ArrayList<>();
        try (InputStream input = Files.newInputStream(output)) {
            byte[] buffer = new byte[32768];
            long run = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                for (int index = 0; index < count; index++) {
                    if (buffer[index] == 'x') {
                        run++;
                    } else {
                        if (run > 1024) bodyLengths.add(run);
                        run = 0;
                    }
                }
            }
        }
        assertEquals(List.of(40L * 1024 * 1024, 50L * 1024 * 1024), bodyLengths);
    }

    @Test
    @EnabledIfSystemProperty(named = "mock.export.large-test", matches = "true")
    void exportsThousandSmallRequests() throws Exception {
        try (Connection connection = source.getConnection();
             PreparedStatement request = connection.prepareStatement(
                     "INSERT INTO t_mock_request(id,group_id,request_path) VALUES (?,1001,?)");
             PreparedStatement response = connection.prepareStatement(
                     "INSERT INTO t_mock_data(id,group_id,request_id,response_body) VALUES (?,1001,?,?)")) {
            for (int index = 1; index <= 1000; index++) {
                request.setInt(1, index);
                request.setString(2, "/small/" + index);
                request.addBatch();
                response.setInt(1, index);
                response.setInt(2, index);
                response.setString(3, "small response");
                response.addBatch();
            }
            request.executeBatch();
            response.executeBatch();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long start = System.nanoTime();
        exporter.writeGroups(List.of(group), output);
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        assertEquals(1000, JsonUtils.getMapper().readTree(output.toByteArray()).path("groups")
                .get(0).path("requests").size());
        System.out.println("EXPORT_SMALL requests=1000 responses=1000 elapsedMillis=" + elapsed);
    }

    private MockRequest addRequest(int id, Integer modifyFrom) {
        MockRequest request = new MockRequest();
        request.setId(id);
        request.setGroupId(group.getId());
        request.setRequestPath("/export/" + id);
        request.setModifyFrom(modifyFrom);
        requests.save(request);
        return request;
    }

    private void insertLargeBody(int id, int size) throws Exception {
        try (Connection connection = source.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO t_mock_data(id,group_id,request_id,response_body) VALUES (?,1001,1001,?)")) {
            statement.setInt(1, id);
            statement.setCharacterStream(2, new Reader() {
                int remaining = size;
                @Override public int read(char[] buffer, int offset, int length) {
                    if (remaining == 0) return -1;
                    int count = Math.min(length, remaining);
                    java.util.Arrays.fill(buffer, offset, offset + count, 'x');
                    remaining -= count;
                    return count;
                }
                @Override public void close() { }
            }, size);
            statement.executeUpdate();
        }
    }

    public static class LargeFixture {
        public static void main(String[] args) throws Exception {
            MockGroupExportServiceTest test = new MockGroupExportServiceTest();
            test.source = new DriverManagerDataSource(args[0], "sa", "");
            try (Connection connection = test.source.getConnection()) {
                test.insertLargeBody(1001, 40 * 1024 * 1024);
                test.insertLargeBody(1002, 50 * 1024 * 1024);
            }
            System.out.println("EXPORT_FIXTURE maxHeap=" + Runtime.getRuntime().maxMemory());
        }
    }

}
