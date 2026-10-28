package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.mapper.mock.MockScenarioMapper;
import com.fugary.simple.mock.service.impl.mock.MockScenarioServiceImpl;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockScenarioQueryVo;
import com.fugary.simple.mock.web.vo.query.SimplePage;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockScenarioControllerTest {

    private SqlSession session;
    private MockScenarioController controller;
    private MockProjectService projectService;
    private MockGroup group;

    @BeforeEach
    void setUp() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:scenario-" + UUID.randomUUID(), "sa", "");
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig()
                .setDbConfig(new GlobalConfig.DbConfig().setTablePrefix("t_")));
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(MockScenarioMapper.class);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(true);
        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE t_mock_group (id INT PRIMARY KEY)");
            statement.execute("CREATE TABLE t_mock_request (id INT PRIMARY KEY)");
        }
        ScriptUtils.executeSqlScript(session.getConnection(),
                new ClassPathResource("db/h2/migration/V2_0_41__add_mock_scenario.sql"));
        MockScenarioMapper mapper = session.getMapper(MockScenarioMapper.class);
        for (int id = 1; id <= 25; id++) {
            MockScenario scenario = new MockScenario();
            scenario.setId(id);
            scenario.setGroupId(1);
            scenario.setScenarioName("scenario-" + id);
            scenario.setDescription(id % 2 == 0 ? "target response" : "plain");
            scenario.setStatus(id % 3 == 0 ? 0 : 1);
            mapper.insert(scenario);
        }
        MockScenario otherGroup = new MockScenario();
        otherGroup.setId(99);
        otherGroup.setGroupId(2);
        otherGroup.setScenarioName("scenario-other");
        otherGroup.setDescription("target response");
        otherGroup.setStatus(0);
        mapper.insert(otherGroup);

        MockScenarioServiceImpl scenarioService = new MockScenarioServiceImpl();
        ReflectionTestUtils.setField(scenarioService, "baseMapper", mapper);
        MockGroupService groupService = mock(MockGroupService.class);
        projectService = mock(MockProjectService.class);
        group = new MockGroup();
        group.setId(1);
        group.setUserName("owner");
        group.setProjectId(1);
        when(groupService.getById(1)).thenReturn(group);
        when(projectService.loadMockProject("owner", 1, null)).thenReturn(new MockProject());
        when(projectService.hasGroupAuthority(group, MockConstants.AUTHORITY_READABLE)).thenReturn(true);
        controller = new MockScenarioController();
        ReflectionTestUtils.setField(controller, "mockScenarioService", scenarioService);
        ReflectionTestUtils.setField(controller, "mockGroupService", groupService);
        ReflectionTestUtils.setField(controller, "mockProjectService", projectService);
    }

    @AfterEach
    void tearDown() {
        if (session != null) session.close();
    }

    @Test
    void pagesHaveStableOrderAndOnlyContainTheRequestedGroup() {
        for (int number = 1; number <= 3; number++) {
            SimpleResult<List<MockScenario>> result = controller.search(query(number, 10));
            assertEquals(IntStream.rangeClosed((number - 1) * 10 + 1, Math.min(number * 10, 25))
                    .boxed().collect(Collectors.toList()), ids(result));
            assertEquals(25, result.getPage().getTotalCount());
            assertEquals(3, result.getPage().getPageCount());
            assertEquals(number, result.getPage().getPageNumber());
        }
    }

    @Test
    void keywordAndStatusAreAppliedBeforePaginationWithoutLeakingOtherGroups() {
        MockScenarioQueryVo query = query(2, 2);
        query.setKeyword(" target ");
        query.setStatus(0);
        SimpleResult<List<MockScenario>> result = controller.search(query);
        assertEquals(List.of(18, 24), ids(result));
        assertEquals(4, result.getPage().getTotalCount());
        assertEquals(2, result.getPage().getPageCount());

        query.setKeyword("scenario-25");
        query.setStatus(null);
        query.getPage().setPageNumber(1);
        assertEquals(List.of(25), ids(controller.search(query)));
    }

    @Test
    void emptyAndOutOfRangePagesKeepAccurateTotals() {
        MockScenarioQueryVo query = query(4, 10);
        SimpleResult<List<MockScenario>> result = controller.search(query);
        assertTrue(result.getResultData().isEmpty());
        assertEquals(25, result.getPage().getTotalCount());
        query.setKeyword("missing");
        result = controller.search(query);
        assertTrue(result.getResultData().isEmpty());
        assertEquals(0, result.getPage().getTotalCount());
    }

    @Test
    void queriesWithoutPageKeepAllDropdownOptions() {
        MockScenarioQueryVo query = query(1, 10);
        query.setPage(null);
        SimpleResult<List<MockScenario>> result = controller.search(query);
        assertEquals(25, result.getResultData().size());
        assertNull(result.getPage());
    }

    @Test
    void paginatedSearchStillRequiresReadPermission() {
        when(projectService.hasGroupAuthority(group, MockConstants.AUTHORITY_READABLE)).thenReturn(false);
        assertEquals(MockErrorConstants.CODE_403, controller.search(query(1, 10)).getCode());
    }

    private MockScenarioQueryVo query(int number, int size) {
        MockScenarioQueryVo query = new MockScenarioQueryVo();
        query.setGroupId(1);
        query.setPage(new SimplePage(number, size, 0, 0));
        return query;
    }

    private List<Integer> ids(SimpleResult<List<MockScenario>> result) {
        return result.getResultData().stream().map(MockScenario::getId).collect(Collectors.toList());
    }
}
