package com.fugary.simple.mock.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.mapper.mock.AiConfigMapper;
import com.fugary.simple.mock.mapper.mock.AiLogMapper;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;
import com.fugary.simple.mock.service.impl.ai.AiServiceImpl;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.controllers.admin.AiController;
import com.fugary.simple.mock.web.vo.query.AiConfigQueryVo;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;
import com.fugary.simple.mock.web.vo.query.SimplePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "mybatis-plus.global-config.db-config.update-strategy=always")
@Transactional
class AiIntegrationTest {
    @Autowired
    private AiController controller;
    @Autowired
    private AiConfigMapper configs;
    @Autowired
    private AiLogMapper logs;
    @Autowired
    private AiConfigService configService;
    @Autowired
    private AiLogService logService;

    private void login(String name) {
        MockUser user = new MockUser();
        user.setUserName(name);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(MockConstants.MOCK_USER_KEY, user);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    private AiConfig createConfig(String name) {
        AiConfig config = new AiConfig();
        config.setConfigName(name);
        config.setBaseUrl("https://example.com/v1");
        config.setProvider("openai");
        config.setDefaultModel("test-model");
        config.setApiKey("private-key");
        config.setModifier("forged-user");
        config.setModifyDate(new Date(0));
        return controller.save(config).getResultData();
    }

    private AiChatRequest request() {
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("test");
        return request;
    }

    @Test
    void unchangedSavesKeepVersionAndAuditButChangesAndRollbacksCreateVersions() {
        login("admin");
        AiConfig original = configs.selectById(createConfig("Unchanged config").getId());
        AiConfig input = SimpleMockUtils.copy(original, AiConfig.class);
        input.setConfigName(" Unchanged config ");
        input.setProvider(" OPENAI ");
        input.setBaseUrl(" https://example.com/v1/// ");
        input.setDefaultModel(" test-model ");
        input.setApiKey("  ");
        input.setStatus(null);
        input.setIsDefault(null);
        input.setVersion(999);
        input.setModifier("forged-user");
        input.setModifyDate(new Date(0));
        AiConfig unchanged = configService.saveConfig(input);
        assertEquals(original, unchanged);
        assertEquals(original, configs.selectById(original.getId()));
        assertEquals(0, configService.histories(original.getId(), new AiConfigQueryVo()).getTotal());

        List<Consumer<AiConfig>> changes = List.of(
                value -> value.setConfigName("Changed name"),
                value -> value.setProvider("anthropic"),
                value -> value.setBaseUrl("https://different.example.com/v1"),
                value -> value.setApiKey("different-key"),
                value -> value.setDefaultModel("different-model"),
                value -> value.setStatus(0),
                value -> value.setIsDefault(1));
        int version = original.getVersion();
        for (Consumer<AiConfig> change : changes) {
            AiConfig updated = configs.selectById(original.getId());
            change.accept(updated);
            AiConfig saved = configService.saveConfig(updated);
            assertEquals(++version, saved.getVersion());
            assertEquals(version - 1, configService.histories(original.getId(), new AiConfigQueryVo()).getTotal());
            assertEquals(saved, configService.saveConfig(saved));
            assertEquals(version - 1, configService.histories(original.getId(), new AiConfigQueryVo()).getTotal());
        }
        AiConfig snapshot = configService.histories(original.getId(), new AiConfigQueryVo()).getRecords().get(0);
        configService.recover(snapshot.getId());
        // 回滚目标与当前内容相同，也应记录用户的主动回滚操作。
        assertEquals(version + 2, configService.recover(snapshot.getId()).getVersion());
    }

    @Test
    void databaseRejectsTwoCurrentDefaultsButAllowsDefaultSnapshots() {
        login("admin");
        AiConfig first = createConfig("Default constraint first");
        first.setIsDefault(1);
        configService.saveConfig(first);
        AiConfig second = createConfig("Default constraint second");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> configs.update(null, com.baomidou.mybatisplus.core.toolkit.Wrappers.<AiConfig>update()
                        .eq("id", second.getId()).set("is_default", 1)));
        assertEquals(first.getId(), configService.getDefaultConfig().getId());
        first.setDefaultModel("changed-model");
        configService.saveConfig(first);
        assertEquals(1, configService.histories(first.getId(), new AiConfigQueryVo())
                .getRecords().get(0).getIsDefault());
    }

    @Test
    void rollbackRestoresNullKeyAcrossConnectionChanges() {
        login("admin");
        AiConfig input = new AiConfig();
        input.setConfigName("No authentication");
        input.setProvider("openai");
        input.setBaseUrl("http://localhost:11434/v1");
        input.setDefaultModel("local-model");
        AiConfig current = configService.saveConfig(input);
        current.setApiKey("new-key");
        current.setBaseUrl("https://example.com/v1");
        current = configService.saveConfig(current);
        AiConfig snapshot = configService.histories(current.getId(), new AiConfigQueryVo()).getRecords().get(0);
        AiConfig restored = configService.recover(snapshot.getId());
        assertEquals("http://localhost:11434/v1", restored.getBaseUrl());
        assertNull(restored.getApiKey());
        assertNull(configs.selectById(current.getId()).getApiKey());
        assertEquals(3, restored.getVersion());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentDefaultSelectionsCannotCommitTwoDefaults() throws Exception {
        login("admin");
        AiConfig first = createConfig("Concurrent first");
        AiConfig second = createConfig("Concurrent second");
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.List<java.util.concurrent.Future<Boolean>> results = new java.util.ArrayList<>();
            for (AiConfig config : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    login("admin");
                    try {
                        barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
                        config.setIsDefault(1);
                        configService.saveConfig(config);
                        return true;
                    } catch (org.springframework.dao.DataAccessException | AiException expectedConflict) {
                        return false;
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                }));
            }
            int successes = 0;
            for (java.util.concurrent.Future<Boolean> result : results) {
                if (result.get(20, java.util.concurrent.TimeUnit.SECONDS)) successes++;
            }
            assertTrue(successes >= 1);
            assertEquals(1, configs.selectCount(com.baomidou.mybatisplus.core.toolkit.Wrappers.<AiConfig>query()
                    .isNull("modify_from").eq("is_default", 1)));
            assertNotNull(configService.getDefaultConfig());
        } finally {
            executor.shutdownNow();
            configService.deleteConfig(first.getId());
            configService.deleteConfig(second.getId());
        }
    }

    @Test
    void defaultSwitchRecordsDisplacedConfigAndExcludesSnapshots() {
        login("admin");
        AiConfig first = createConfig("First default");
        first.setIsDefault(1);
        configService.saveConfig(first);
        AiConfig second = createConfig("Second default");
        second.setIsDefault(1);
        configService.saveConfig(second);
        AiConfig displaced = configService.getConfig(first.getId());
        assertEquals(3, displaced.getVersion());
        assertEquals(0, displaced.getIsDefault());
        AiConfig snapshot = configService.histories(first.getId(), new AiConfigQueryVo()).getRecords().get(0);
        assertEquals(1, snapshot.getIsDefault());
        assertEquals(second.getId(), configService.getDefaultConfig().getId());
        configService.recover(snapshot.getId());
        assertEquals(first.getId(), configService.getDefaultConfig().getId());
        assertEquals(0, configService.getConfig(second.getId()).getIsDefault());
        assertTrue(configService.search(new AiConfigQueryVo()).getRecords().stream()
                .allMatch(config -> config.getModifyFrom() == null));
    }

    @Test
    void configHistoryPreservesKeysAndRestrictsAccess() {
        login("admin");
        AiConfig current = createConfig("History");
        current.setApiKey("new-key");
        current.setDefaultModel("new-model");
        current = configService.saveConfig(current);
        assertEquals(2, current.getVersion());
        List<AiConfig> history = configService.histories(current.getId(), new AiConfigQueryVo()).getRecords();
        assertEquals(1, history.size());
        Integer historyId = history.get(0).getId();
        assertEquals("private-key", history.get(0).getApiKey());
        assertEquals("private-key", configService.historyDiff(current.getId()).get("original").getApiKey());
        assertThrows(AiException.class, () -> configService.getConfig(historyId));
        AiConfig restored = configService.recover(historyId);
        assertEquals(3, restored.getVersion());
        assertEquals("private-key", restored.getApiKey());
        assertEquals("test-model", restored.getDefaultModel());
        login("ordinary-user");
        Integer currentId = current.getId();
        assertEquals(403, assertThrows(AiException.class,
                () -> controller.histories(currentId, new AiConfigQueryVo())).getCode());
        assertEquals(403, assertThrows(AiException.class,
                () -> controller.historyDiff(historyId)).getCode());
        assertEquals(403, assertThrows(AiException.class,
                () -> controller.recover(historyId)).getCode());
        assertThrows(AiException.class, () -> controller.getConfig(historyId));
        assertNull(controller.getConfig(currentId).getResultData().getApiKey());
        login("admin");
        configService.deleteConfig(currentId);
        assertNull(configs.selectById(historyId));
    }

    @Test
    void longErrorDetailsDoNotPreventFailureLogging() {
        login("admin");
        AiConfig config = createConfig("Error details");
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configService, logService, List.of(provider));
        AiException failure = new AiException(500, "simple.ai.httpError", 429)
                .withDetail("Quota exceeded " + "x".repeat(1900));
        when(provider.chat(any(), any())).thenThrow(failure);
        assertSame(failure, assertThrows(AiException.class, () -> service.test(config.getId(), request())));
        AiLogQueryVo query = new AiLogQueryVo();
        query.setConfigId(config.getId());
        AiLog entry = logService.search(query).getRecords().get(0);
        assertEquals(0, entry.getStatus());
        assertEquals(512, entry.getErrorMessage().length());
        assertTrue(entry.getErrorMessage().contains("Quota exceeded"));
        assertEquals(failure.getErrorMessage(), controller.handleError(failure).getMessage());
    }

    @Test
    void normalizesConnectionsAndModelsForSavingLoadingAndTesting() {
        login("admin");
        AiConfig draft = new AiConfig();
        draft.setConfigName("  Normalized config  ");
        draft.setProvider(" OPENAI ");
        draft.setBaseUrl(" https://example.com/v1/// ");
        draft.setApiKey(" private-key ");
        draft.setDefaultModel(" test-model ");
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        AiService service = new AiServiceImpl(configService, logService, Collections.singletonList(provider));
        when(provider.loadModels(any())).thenAnswer(call -> {
            AiConfig connection = call.getArgument(0);
            assertEquals("https://example.com/v1", connection.getBaseUrl());
            assertEquals("private-key", connection.getApiKey());
            assertEquals("openai", connection.getProvider());
            return List.of("test-model");
        });
        when(provider.chat(any(), any())).thenAnswer(call -> {
            AiConfig connection = call.getArgument(0);
            assertEquals("test-model", connection.getDefaultModel());
            assertEquals("https://example.com/v1", connection.getBaseUrl());
            assertEquals("private-key", connection.getApiKey());
            AiChatResponse response = new AiChatResponse();
            response.setContent("answer");
            return response;
        });
        assertEquals(List.of("test-model"), service.loadModels(draft));
        assertEquals("answer", service.testDraft(draft, request(), null).getContent());
        AiConfig saved = configService.saveConfig(draft);
        AiConfig stored = configs.selectById(saved.getId());
        assertEquals("Normalized config", stored.getConfigName());
        assertEquals("test-model", stored.getDefaultModel());
        assertEquals("https://example.com/v1", stored.getBaseUrl());
        assertEquals(List.of("test-model"), service.loadModels(saved.getId()));
        assertEquals("answer", service.test(saved.getId(), request()).getContent());
        draft.setId(saved.getId());
        draft.setApiKey(" ");
        assertEquals("private-key", configService.resolveConnection(draft).getApiKey());
        assertEquals("private-key", configService.saveConfig(draft).getApiKey());
        draft.setBaseUrl(" https://different.example.com/v1/ ");
        assertThrows(AiException.class, () -> configService.resolveConnection(draft));
        assertThrows(AiException.class, () -> configService.saveConfig(draft));
    }

    @Test
    void ordinaryUsersCanUseEnabledConfigsButCannotManageOrOverrideConnections() {
        login("admin");
        AiConfig config = createConfig("Shared model");
        AiChatProvider provider = mock(AiChatProvider.class);
        when(provider.getProvider()).thenReturn(AiProvider.OPENAI);
        when(provider.loadModels(any())).thenReturn(Collections.singletonList("test-model"));
        AiChatResponse answer = new AiChatResponse();
        answer.setContent("answer");
        when(provider.chat(any(), any())).thenReturn(answer);
        when(provider.chat(any(), any(), any())).thenReturn(answer);
        AiService service = new AiServiceImpl(configService, logService, Collections.singletonList(provider));
        login("shared-user");
        assertEquals(Collections.singletonList("test-model"), service.loadModels(config.getId()));
        assertEquals("answer", service.test(config.getId(), request()).getContent());
        assertEquals("answer", service.test(config.getId(), request(), new AiStreamSession(text -> {})).getContent());
        assertEquals(2, controller.searchLogs(logQuery(config.getId())).getResultData().size());
        assertEquals(403, assertThrows(AiException.class, () -> controller.save(config)).getCode());
        assertEquals(403, assertThrows(AiException.class, () -> createConfig("Forbidden")).getCode());
        assertEquals(403, assertThrows(AiException.class, () -> controller.deleteConfig(config.getId())).getCode());
        config.setBaseUrl("https://attacker.example.com/v1");
        assertEquals(403, assertThrows(AiException.class, () -> service.loadModels(config)).getCode());
        login("admin");
        AiConfig disabled = configs.selectById(config.getId());
        disabled.setStatus(0);
        controller.save(disabled);
        login("shared-user");
        clearInvocations(provider);
        assertNull(controller.getConfig(config.getId()).getResultData().getApiKey());
        assertEquals(403, assertThrows(AiException.class, () -> service.loadModels(config.getId())).getCode());
        assertEquals(403, assertThrows(AiException.class, () -> service.test(config.getId(), request())).getCode());
        assertEquals(403, assertThrows(AiException.class, () -> controller.streamTest(config.getId(), request())).getCode());
        verifyNoInteractions(provider);
        login("other-user");
        assertTrue(controller.searchLogs(logQuery(config.getId())).getResultData().isEmpty());
        RequestContextHolder.resetRequestAttributes();
        assertEquals(401, assertThrows(AiException.class, () -> controller.getConfig(config.getId())).getCode());
        assertEquals(401, assertThrows(AiException.class, () -> controller.searchConfigs(new AiConfigQueryVo())).getCode());
        assertEquals(401, assertThrows(AiException.class, () -> configService.getDefaultConfig()).getCode());
    }

    @Test
    void helpDefaultIsEnabledAndSharedByAllUsers() {
        login("admin");
        assertNull(configService.getDefaultConfig());
        AiConfig config = createConfig("Help model");
        config.setIsDefault(1);
        config.setStatus(1);
        controller.save(config);
        assertEquals(config.getId(), configService.getDefaultConfig().getId());
        login("ai-help-bob");
        assertEquals(config.getId(), configService.getDefaultConfig().getId());
        assertThrows(AiException.class, () -> controller.save(config));
        login("admin");
        AiConfig replacement = createConfig("Replacement default");
        replacement.setIsDefault(1);
        controller.save(replacement);
        assertEquals(0, configs.selectById(config.getId()).getIsDefault());
        login("ai-help-bob");
        assertEquals(replacement.getId(), configService.getDefaultConfig().getId());
        login("admin");
        replacement.setStatus(0);
        controller.save(replacement);
        config.setStatus(0);
        assertNull(configService.getDefaultConfig());
    }

    @Test
    void sharedConfigurationsKeepLogsPrivateAndPreserveHistory() {
        login("admin");
        AiConfig alice = createConfig("Alice model");
        AiConfig connection = new AiConfig();
        connection.setId(alice.getId());
        connection.setProvider("OPENAI");
        connection.setBaseUrl("https://example.com/v1");
        AiConfig unsaved = new AiConfig();
        unsaved.setProvider("openai");
        unsaved.setBaseUrl("https://example.com/v1");
        assertNull(configService.resolveConnection(unsaved).getId());
        assertEquals("private-key", configService.resolveConnection(connection).getApiKey());
        assertNull(configService.resolveConnection(connection).getDefaultModel());
        connection.setProvider("anthropic");
        assertThrows(AiException.class, () -> configService.resolveConnection(connection));
        connection.setProvider("openai");
        connection.setBaseUrl("https://different.example.com/v1");
        assertThrows(AiException.class, () -> configService.resolveConnection(connection));
        connection.setBaseUrl("https://example.com/v1");
        assertNotEquals("forged-user", configs.selectById(alice.getId()).getModifier());
        assertNotEquals(new Date(0), configs.selectById(alice.getId()).getModifyDate());
        AiConfig edited = configs.selectById(alice.getId());
        edited.setApiKey("");
        edited.setConfigName("Alice edited");
        controller.save(edited);
        assertEquals("private-key", configs.selectById(alice.getId()).getApiKey());
        edited.setBaseUrl("https://changed.example.com/v1");
        assertThrows(AiException.class, () -> controller.save(edited));
        edited.setBaseUrl("https://example.com/v1");
        edited.setProvider("gemini");
        assertThrows(AiException.class, () -> controller.save(edited));
        edited.setApiKey("new-key");
        controller.save(edited);
        assertEquals("new-key", configs.selectById(alice.getId()).getApiKey());

        AiLog entry = new AiLog();
        entry.setUserName("admin");
        entry.setConfigId(alice.getId());
        entry.setConfigName("Alice model");
        entry.setPrompt("private prompt");
        entry.setResponseText("private answer");
        entry.setRawResponse("{\"answer\":\"private answer\"}");
        entry.setExecutionDetail("{\"version\":1}");
        entry.setProvider("openai");
        entry.setStatus(1);
        logs.insert(entry);

        AiConfig bob = createConfig("Second public model");
        edited.setStatus(0);
        controller.save(edited);
        login("ai-test-bob");
        assertThrows(AiException.class, () -> configService.resolveConnection(connection));
        connection.setApiKey("own-key");
        assertThrows(AiException.class, () -> configService.resolveConnection(connection));
        assertThrows(AiException.class, () -> controller.loadModels(alice.getId()));
        assertThrows(AiException.class, () -> createConfig("Forbidden"));
        assertThrows(AiException.class, () -> controller.save(edited));
        assertNull(controller.getConfig(alice.getId()).getResultData().getApiKey());
        assertThrows(AiException.class, () -> controller.deleteConfig(alice.getId()));
        assertThrows(AiException.class, () -> controller.test(alice.getId(), request()));
        assertThrows(AiException.class, () -> controller.getLog(entry.getId()));
        assertThrows(AiException.class, () -> controller.deleteLog(entry.getId()));
        AiConfigQueryVo query = new AiConfigQueryVo();
        query.setPage(new SimplePage(1L, 1L, 0L, 0L));
        assertEquals(1, controller.searchConfigs(query).getResultData().size());
        assertEquals(bob.getId(), controller.searchConfigs(query).getResultData().get(0).getId());
        assertTrue(controller.searchLogs(logQuery(alice.getId())).getResultData().isEmpty());

        login("admin");
        assertEquals(bob.getId(), controller.getConfig(bob.getId()).getResultData().getId());
        assertEquals(1, controller.searchLogs(logQuery(alice.getId())).getResultData().size());
        assertTrue(controller.searchLogs(logQuery(bob.getId())).getResultData().isEmpty());
        assertNull(controller.searchLogs(logQuery(alice.getId())).getResultData().get(0).getPrompt());
        assertNull(controller.searchLogs(logQuery(alice.getId())).getResultData().get(0).getRawResponse());
        assertNull(controller.searchLogs(logQuery(alice.getId())).getResultData().get(0).getExecutionDetail());
        assertEquals(entry.getRawResponse(), controller.getLog(entry.getId()).getResultData().getRawResponse());
        assertEquals(entry.getExecutionDetail(), controller.getLog(entry.getId()).getResultData().getExecutionDetail());
        controller.deleteConfig(alice.getId());
        assertEquals("private answer", controller.getLog(entry.getId()).getResultData().getResponseText());
        controller.deleteLog(entry.getId());
        assertNull(logs.selectById(entry.getId()));
    }

    @Test
    void configurationFiltersWorkForPublicReadOnlyAccess() {
        login("admin");
        AiConfig target = createConfig("Team primary");
        target.setProvider("anthropic");
        target.setApiKey("private-key");
        target.setDefaultModel("claude-example");
        target.setStatus(0);
        controller.save(target);
        createConfig("Team fallback");
        login("ai-filter-bob");

        AiConfigQueryVo query = new AiConfigQueryVo();
        query.setKeyword("primary");
        assertEquals(target.getId(), controller.searchConfigs(query).getResultData().get(0).getId());
        query.setKeyword(null);
        query.setProvider("anthropic");
        assertEquals(1, controller.searchConfigs(query).getResultData().size());
        query.setProvider(null);
        query.setDefaultModel("example");
        assertEquals(1, controller.searchConfigs(query).getResultData().size());
        query.setProvider("anthropic");
        query.setKeyword("Team");
        query.setStatus(0);
        assertEquals(1, controller.searchConfigs(query).getResultData().size());
        assertEquals(target.getId(), controller.searchConfigs(query).getResultData().get(0).getId());
        query.setStatus(1);
        assertTrue(controller.searchConfigs(query).getResultData().isEmpty());
        query.setKeyword(null);
        query.setProvider(" ");
        query.setDefaultModel(" ");
        query.setStatus(null);
        assertEquals(2, controller.searchConfigs(query).getResultData().size());
    }


    @Test
    void adminCanFilterAndManageAllLogsWhileOrdinaryUsersStayScoped() {
        login("admin");
        AiConfig config = createConfig("Shared history");
        Map<String, Integer> ids = new HashMap<>();
        for (String name : List.of("admin", "log-alice", "log-bob")) {
            AiLog entry = new AiLog();
            entry.setConfigId(config.getId());
            entry.setConfigName(config.getConfigName());
            entry.setUserName(name);
            entry.setStatus(1);
            entry.setRawResponse("private-" + name);
            logs.insert(entry);
            ids.put(name, entry.getId());
        }
        AiLogQueryVo query = logQuery(config.getId());
        assertEquals(3, controller.searchLogs(query).getResultData().size());
        query.setUserName(" log-bob ");
        assertEquals(ids.get("log-bob"), controller.searchLogs(query).getResultData().get(0).getId());
        query.setUserName("log-b");
        assertTrue(controller.searchLogs(query).getResultData().isEmpty());
        query.setUserName(" ");
        assertEquals(3, controller.searchLogs(query).getResultData().size());
        assertEquals("private-log-bob", controller.getLog(ids.get("log-bob")).getResultData().getRawResponse());
        query.setUserName("x".repeat(256));
        assertEquals(400, assertThrows(AiException.class, () -> controller.searchLogs(query)).getCode());

        login("log-alice");
        query.setUserName("log-bob");
        assertEquals(1, controller.searchLogs(query).getResultData().size());
        assertEquals(ids.get("log-alice"), controller.searchLogs(query).getResultData().get(0).getId());
        assertEquals(404, assertThrows(AiException.class, () -> controller.getLog(ids.get("log-bob"))).getCode());
        assertEquals(404, assertThrows(AiException.class, () -> controller.deleteLog(ids.get("log-bob"))).getCode());
        assertEquals("private-log-alice", controller.getLog(ids.get("log-alice")).getResultData().getRawResponse());
        controller.deleteLog(ids.get("log-alice"));
        assertNull(logs.selectById(ids.get("log-alice")));

        login("admin");
        controller.deleteLog(ids.get("admin"));
        controller.deleteConfig(config.getId());
        // 只剩其他用户的历史记录，管理员仍能从配置选项筛选。
        assertTrue(controller.options().getResultData().stream().anyMatch(option -> option.getId().equals(config.getId()) && option.isDeleted()));
        login("log-alice");
        assertFalse(controller.options().getResultData().stream().anyMatch(option -> option.getId().equals(config.getId())));
        login("admin");
        controller.deleteLog(ids.get("log-bob"));
        assertNull(logs.selectById(ids.get("log-bob")));

        RequestContextHolder.resetRequestAttributes();
        query.setUserName("admin");
        assertEquals(401, assertThrows(AiException.class, () -> controller.searchLogs(query)).getCode());
        assertEquals(401, assertThrows(AiException.class, () -> controller.getLog(ids.get("log-bob"))).getCode());
        assertEquals(401, assertThrows(AiException.class, () -> controller.deleteLog(ids.get("log-bob"))).getCode());
    }

    private AiLogQueryVo logQuery(Integer configId) {
        AiLogQueryVo query = new AiLogQueryVo();
        query.setConfigId(configId);
        return query;
    }

    @Test
    void logFiltersAndOptionsIncludeHistoryButNeverAnotherOwner() {
        login("admin");
        AiConfig config = createConfig("history config");
        for (int i = 0; i < 11; i++) {
            createConfig("choice " + i);
        }
        AiLog entry = new AiLog();
        entry.setUserName("admin");
        entry.setConfigId(config.getId());
        entry.setConfigName(config.getConfigName());
        entry.setModel("test-model");
        entry.setPrompt("find-prompt");
        entry.setResponseText("find-answer");
        entry.setErrorMessage("find-error");
        entry.setStatus(0);
        entry.setCreateDate(new Date(1700000000000L));
        logs.insert(entry);
        controller.deleteConfig(config.getId());
        assertEquals(12, controller.options().getResultData().size());
        assertTrue(controller.options().getResultData().stream().anyMatch(option -> option.getId().equals(config.getId()) && option.isDeleted()));
        AiLogQueryVo query = logQuery(config.getId());
        query.setStatus(0);
        query.setModel("model");
        query.setStartDate(entry.getCreateDate());
        query.setEndDate(entry.getCreateDate());
        for (String keyword : new String[]{"history", "find-prompt", "find-answer", "find-error"}) {
            query.setKeyword(keyword);
            assertEquals(1, controller.searchLogs(query).getResultData().size());
        }
        query.setStatus(1);
        assertTrue(controller.searchLogs(query).getResultData().isEmpty());
        query.setStatus(null);
        query.setStartDate(new Date(entry.getCreateDate().getTime() + 1));
        assertThrows(AiException.class, () -> controller.searchLogs(query));
        query.setEndDate(null);
        assertTrue(controller.searchLogs(query).getResultData().isEmpty());
        query.setStartDate(null);
        query.setKeyword(" ");
        query.setModel(" ");
        query.setConfigId(null);
        assertEquals(1, controller.searchLogs(query).getResultData().size());
        login("log-filter-bob");
        assertTrue(controller.searchLogs(query).getResultData().isEmpty());
        assertEquals(11, controller.options().getResultData().size());
        assertFalse(controller.options().getResultData().stream().anyMatch(option -> option.getId().equals(config.getId())));
    }


    @Test
    void onlyAdminDetailExposesKeyAndPublicResponsesNeverDo() throws Exception {
        login("admin");
        AiConfig config = createConfig("private config");
        ObjectMapper mapper = new ObjectMapper();
        AiConfig detail = controller.getConfig(config.getId()).getResultData();
        assertEquals("private-key", mapper.readTree(mapper.writeValueAsString(detail)).path("apiKey").asText());
        assertFalse(detail.toString().contains("private-key"));
        String listJson = mapper.writeValueAsString(controller.searchConfigs(new AiConfigQueryVo()));
        assertFalse(listJson.contains("apiKey"));
        assertFalse(listJson.contains("private-key"));
        String optionsJson = mapper.writeValueAsString(controller.options());
        assertFalse(optionsJson.contains("apiKey"));
        assertFalse(optionsJson.contains("private-key"));
        assertFalse(mapper.writeValueAsString(controller.save(config)).contains("apiKey"));
        assertEquals("private-key", configs.selectById(config.getId()).getApiKey());
        login("key-detail-stranger");
        assertNull(controller.getConfig(config.getId()).getResultData().getApiKey());
        assertFalse(mapper.writeValueAsString(controller.getConfig(config.getId())).contains("private-key"));
        assertFalse(mapper.writeValueAsString(controller.searchConfigs(new AiConfigQueryVo())).contains("private-key"));
        assertFalse(mapper.writeValueAsString(controller.options()).contains("private-key"));
        login("admin");
        assertEquals("private-key", controller.getConfig(config.getId()).getResultData().getApiKey());
    }

}
