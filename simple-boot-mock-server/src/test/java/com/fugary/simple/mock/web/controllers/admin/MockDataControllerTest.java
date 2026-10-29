package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.config.GlobalTransactionConfig;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.mapper.mock.MockDataMapper;
import com.fugary.simple.mock.service.impl.mock.MockDataServiceImpl;
import com.fugary.simple.mock.service.impl.mock.MockRequestServiceImpl;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.web.vo.query.MockHistoryVo;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Statement;
import java.util.UUID;
import java.util.Map;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockDataControllerTest {
    private SqlSession session;
    private MockDataServiceImpl service;
    private MockDataController controller;
    private TransactionTemplate transactionTemplate;
    private Integer id;
    private final String largeBody = "响应数据".repeat(250000);

    @BeforeEach
    void setUp() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:data-" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        configuration.setEnvironment(new Environment("test", new SpringManagedTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()
                .setTablePrefix("t_").setUpdateStrategy(FieldStrategy.ALWAYS)));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(MockDataMapper.class);
        SqlSessionFactory sessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
        session = sessionFactory.openSession(true);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE t_mock_data (id INT AUTO_INCREMENT PRIMARY KEY, group_id INT, "
                    + "request_id INT, data_name VARCHAR, status_code INT, default_flag INT, content_type VARCHAR, "
                    + "match_pattern VARCHAR, response_format VARCHAR, response_body CLOB, delay INT, description VARCHAR, "
                    + "headers VARCHAR, mock_params VARCHAR, default_charset VARCHAR, post_processor VARCHAR, "
                    + "data_version INT, modify_from INT, creator VARCHAR, create_date TIMESTAMP, modifier VARCHAR, "
                    + "modify_date TIMESTAMP, status INT)");
        }
        MockDataServiceImpl target = new MockDataServiceImpl();
        ReflectionTestUtils.setField(target, "baseMapper", new SqlSessionTemplate(sessionFactory).getMapper(MockDataMapper.class));
        // 使用应用的事务配置和默认 SESSION 缓存，覆盖事务内重复查询同一条数据的情况。
        GlobalTransactionConfig transactionConfig = new GlobalTransactionConfig();
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionTemplate = new TransactionTemplate(transactionManager);
        ReflectionTestUtils.setField(transactionConfig, "transactionManager", transactionManager);
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvisor(transactionConfig.txAdviceAdvisor());
        service = (MockDataServiceImpl) proxyFactory.getProxy();
        controller = new MockDataController();
        ReflectionTestUtils.setField(controller, "mockDataService", service);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        MockProjectService projectService = mock(MockProjectService.class);
        when(projectService.hasDataAuthority(any(MockData.class), anyString())).thenReturn(true);
        when(projectService.hasDataAuthority(anyInt(), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(controller, "mockProjectService", projectService);
        MockData data = new MockData();
        data.setRequestId(1);
        data.setStatus(1);
        data.setStatusCode(200);
        data.setResponseBody(largeBody);
        assertTrue(service.newSaveOrUpdate(data).isSuccess());
        id = data.getId();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void summaryIsBoundedAndDetailAndLegacySearchRemainComplete() {
        MockDataQueryVo query = new MockDataQueryVo();
        query.setRequestId(1);
        query.setSummary(true);
        MockData summary = controller.search(query).getResultData().get(0);
        assertNull(summary.getResponseBody());
        assertEquals(largeBody.substring(0, 61), summary.getResponseBodyPreview());
        assertEquals(largeBody, controller.get(id, true).getResultData().getResponseBody());
        query.setSummary(false);
        assertEquals(largeBody, controller.search(query).getResultData().get(0).getResponseBody());
    }

    @Test
    void patchPreservesMissingFieldsAndAllowsNullAndEmptyString() throws Exception {
        assertTrue(controller.patch(id, Map.of("status", 0)).isSuccess());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        assertEquals(largeBody, service.list().stream().filter(item -> id.equals(item.getModifyFrom()))
                .findFirst().orElseThrow().getResponseBody());
        long count = service.count();
        controller.patch(id, Map.of("status", 0));
        assertEquals(count, service.count(), "Unchanged data must not create history");
        assertTrue(controller.patch(id, Map.of("responseBody", "")).isSuccess());
        assertEquals("", service.getById(id).getResponseBody());
        MockDataQueryVo query = new MockDataQueryVo();
        query.setRequestId(1);
        query.setSummary(true);
        assertEquals("", controller.search(query).getResultData().get(0).getResponseBodyPreview());
        assertTrue(controller.patch(id, Map.of("responseBody", largeBody)).isSuccess());
        assertTrue(controller.patch(id, Collections.singletonMap("responseBody", null)).isSuccess());
        assertNull(service.getById(id).getResponseBody());
        assertNull(controller.search(query).getResultData().get(0).getResponseBodyPreview());
        count = service.count();
        controller.patch(id, Collections.singletonMap("responseBody", null));
        assertEquals(count, service.count(), "Clearing an already empty body must not create history");
    }

    @Test
    void staleOrNullVersionCannotChangeDataOrCreateHistory() throws Exception {
        int staleVersion = service.getById(id).getVersion();
        assertTrue(controller.patch(id, Map.of("dataName", "new name", "version", staleVersion)).isSuccess());
        long count = service.count();
        Map<String, Object> changes = new HashMap<>();
        changes.put("responseBody", null);
        changes.put("version", staleVersion);
        assertFalse(controller.patch(id, changes).isSuccess());
        changes.put("version", null);
        assertFalse(controller.patch(id, changes).isSuccess());
        MockData stored = service.getById(id);
        assertEquals("new name", stored.getDataName());
        assertEquals(largeBody, stored.getResponseBody());
        assertEquals(staleVersion + 1, stored.getVersion());
        assertEquals(count, service.count());
    }

    @Test
    void patchRequiresExistingDataAndWritePermission() throws Exception {
        long count = service.count();
        assertEquals(MockErrorConstants.CODE_404, controller.patch(999, Map.of("status", 0)).getCode());
        MockProjectService projectService = (MockProjectService) ReflectionTestUtils.getField(controller, "mockProjectService");
        when(projectService.hasDataAuthority(any(MockData.class), anyString())).thenReturn(false);
        assertEquals(MockErrorConstants.CODE_403,
                controller.patch(id, Collections.singletonMap("responseBody", null)).getCode());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        assertEquals(count, service.count());
    }

    @Test
    void metadataAndParameterPatchDoNotReturnBodyOrChangeUnrelatedFields() throws Exception {
        MockData metadata = controller.get(id, false).getResultData();
        assertNull(metadata.getResponseBody());
        assertEquals(1, metadata.getRequestId());
        MockData saved = controller.patch(id, Map.of("mockParams", "{}", "requestId", 999,
                "responseBodyPreview", "truncated", "creator", "changed")).getResultData();
        assertNull(saved.getResponseBody());
        MockData stored = service.getById(id);
        assertEquals("{}", stored.getMockParams());
        assertEquals(1, stored.getRequestId());
        assertNull(stored.getCreator());
        assertEquals(largeBody, stored.getResponseBody());
        assertTrue(controller.patch(id, Map.of("matchPattern", "request.params.id === '123'")).isSuccess());
        assertEquals(largeBody, service.getById(id).getResponseBody());
    }

    @Test
    void historySummaryDoesNotTruncateComparisonOrRecovery() throws Exception {
        String modifiedBody = "new-" + largeBody;
        assertTrue(controller.patch(id, Map.of("responseBody", modifiedBody)).isSuccess());
        MockDataQueryVo query = new MockDataQueryVo();
        query.setSummary(true);
        SimpleResult<List<MockData>> histories = controller.histories(query, id);
        MockData history = histories.getResultData().get(0);
        assertNull(history.getResponseBody());
        assertEquals(largeBody.substring(0, 61), history.getResponseBodyPreview());
        MockData current = (MockData) histories.getInfos().get("current");
        assertNull(current.getResponseBody());
        assertEquals(modifiedBody.substring(0, 61), current.getResponseBodyPreview());
        assertEquals(largeBody, controller.get(history.getId(), true).getResultData().getResponseBody());
        query.setSummary(false);
        assertEquals(largeBody, controller.histories(query, id).getResultData().get(0).getResponseBody());

        MockHistoryVo compare = new MockHistoryVo();
        compare.setId(id);
        Map<String, MockData> diff = controller.loadHistoryDiff(compare).getResultData();
        assertEquals(modifiedBody, diff.get("modified").getResponseBody());
        assertEquals(largeBody, diff.get("original").getResponseBody());
        MockHistoryVo recovery = new MockHistoryVo();
        recovery.setId(history.getId());
        assertTrue(controller.recoverFromHistory(recovery).isSuccess());
        assertEquals(largeBody, service.getById(id).getResponseBody());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void patchIgnoresProtectedAndUnknownFieldsEvenWhenExplicitlyNull(boolean nullValues) throws Exception {
        MockData original = service.getById(id);
        original.setGroupId(10);
        original.setCreator("original creator");
        original.setCreateDate(new Date(1000));
        original.setModifier("original modifier");
        original.setModifyDate(new Date(2000));
        assertTrue(service.updateById(original));

        Map<String, Object> changes = new HashMap<>();
        changes.put("id", 999);
        changes.put("groupId", 999);
        changes.put("requestId", 999);
        changes.put("modifyFrom", 999);
        changes.put("creator", "injected creator");
        changes.put("createDate", 1);
        changes.put("modifier", "injected modifier");
        changes.put("modifyDate", 1);
        changes.put("responseBodyPreview", "injected preview");
        changes.put("enabled", false);
        changes.put("unknownField", "ignored");
        if (nullValues) {
            changes.replaceAll((key, value) -> null);
        }
        changes.put("dataName", "updated name");
        changes.put("version", original.getVersion());
        assertTrue(controller.patch(id, changes).isSuccess());

        MockData stored = service.getById(id);
        assertEquals(id, stored.getId());
        assertNull(service.getById(999));
        assertEquals(10, stored.getGroupId());
        assertEquals(1, stored.getRequestId());
        assertNull(stored.getModifyFrom());
        assertEquals(original.getCreator(), stored.getCreator());
        assertEquals(original.getCreateDate(), stored.getCreateDate());
        assertEquals(original.getModifier(), stored.getModifier());
        assertTrue(stored.getModifyDate().after(original.getModifyDate()), "Modification time comes from server auditing");
        assertNull(stored.getResponseBodyPreview());
        assertEquals(1, stored.getStatus());
        assertEquals(largeBody, stored.getResponseBody());
        assertEquals("updated name", stored.getDataName());
        assertEquals(original.getVersion() + 1, stored.getVersion());
    }

    @Test
    void markingSummaryAsDefaultDoesNotPersistPreviewAsBody() throws Exception {
        MockDataQueryVo query = new MockDataQueryVo();
        query.setRequestId(1);
        query.setSummary(true);
        MockData summary = controller.search(query).getResultData().get(0);
        ObjectMapper objectMapper = new ObjectMapper()
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MockData submitted = objectMapper.readValue(objectMapper.writeValueAsString(summary), MockData.class);
        assertNull(submitted.getResponseBodyPreview(), "Preview is a read-only response field");
        submitted.setDefaultFlag(1);
        assertTrue(controller.markDefault(submitted).isSuccess());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        assertEquals(1, service.getById(id).getDefaultFlag());
        MockData detail = controller.get(id, true).getResultData();
        detail.setMatchPattern("request.params.id === '123'");
        assertTrue(service.newSaveOrUpdate(detail).isSuccess());
        assertEquals(largeBody, service.getById(id).getResponseBody());
    }

    @Test
    void switchingAndCancellingDefaultPreserveBodiesAndHistory() {
        MockData second = new MockData();
        second.setRequestId(1);
        second.setResponseBody("second-" + largeBody);
        assertTrue(service.newSaveOrUpdate(second).isSuccess());

        MockData unrelated = new MockData();
        unrelated.setRequestId(2);
        unrelated.setDefaultFlag(1);
        assertTrue(service.newSaveOrUpdate(unrelated).isSuccess());

        MockData change = new MockData();
        change.setId(id);
        change.setRequestId(1);
        change.setVersion(service.getById(id).getVersion());
        change.setDefaultFlag(1);
        change.setResponseBody("must not overwrite the stored body");
        assertTrue(controller.markDefault(change).isSuccess());
        assertEquals(1, service.getById(id).getDefaultFlag());

        change.setId(second.getId());
        change.setVersion(second.getVersion());
        assertTrue(controller.markDefault(change).isSuccess());
        assertEquals(0, service.getById(id).getDefaultFlag());
        assertEquals(1, service.getById(second.getId()).getDefaultFlag());

        change.setVersion(service.getById(second.getId()).getVersion());
        long count = service.count();
        assertTrue(controller.markDefault(change).isSuccess(), "Setting the current default again is a successful no-op");
        assertEquals(count, service.count());
        assertEquals(change.getVersion(), service.getById(second.getId()).getVersion());
        change.setDefaultFlag(0);
        assertTrue(controller.markDefault(change).isSuccess());
        assertEquals(0, service.getById(second.getId()).getDefaultFlag());
        assertEquals(1, service.getById(unrelated.getId()).getDefaultFlag());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        assertEquals("second-" + largeBody, service.getById(second.getId()).getResponseBody());

        List<MockData> histories = service.list().stream().filter(data -> data.getModifyFrom() != null)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(4, histories.size());
        for (MockData history : histories) {
            assertEquals(id.equals(history.getModifyFrom()) ? largeBody : "second-" + largeBody, history.getResponseBody());
        }
        assertTrue(histories.stream().anyMatch(data -> id.equals(data.getModifyFrom()) && data.getDefaultFlag() == null));
        assertTrue(histories.stream().anyMatch(data -> id.equals(data.getModifyFrom()) && Integer.valueOf(1).equals(data.getDefaultFlag())));
    }

    @Test
    void markingDefaultRejectsStaleVersionOrWrongRequestWithoutCreatingHistory() throws Exception {
        MockData change = new MockData();
        change.setId(id);
        change.setRequestId(1);
        change.setVersion(service.getById(id).getVersion());
        change.setDefaultFlag(1);
        assertTrue(controller.patch(id, Map.of("dataName", "updated")).isSuccess());
        long count = service.count();
        assertFalse(controller.markDefault(change).isSuccess());
        change.setVersion(service.getById(id).getVersion());
        change.setRequestId(999);
        assertFalse(controller.markDefault(change).isSuccess());
        assertEquals(count, service.count());
        assertNull(service.getById(id).getDefaultFlag());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        assertEquals("updated", service.getById(id).getDataName());

        change.setRequestId(1);
        change.setVersion(null);
        assertTrue(controller.markDefault(change).isSuccess(), "Legacy requests without version remain supported");
        assertEquals(1, service.getById(id).getDefaultFlag());
    }

    @Test
    void savingRequestParametersWithinTransactionPreservesBodyAndHistory() {
        MockRequestServiceImpl requestService = new MockRequestServiceImpl();
        ReflectionTestUtils.setField(requestService, "mockDataService", service);
        MockData change = new MockData();
        change.setId(id);
        change.setMockParams("{}");
        transactionTemplate.executeWithoutResult(transaction -> assertTrue(requestService.saveMockParams(change)));
        assertEquals("{}", service.getById(id).getMockParams());
        assertEquals(largeBody, service.getById(id).getResponseBody());
        MockData history = service.list().stream().filter(data -> id.equals(data.getModifyFrom()))
                .findFirst().orElseThrow();
        assertNull(history.getMockParams());
        assertEquals(largeBody, history.getResponseBody());
    }
}
