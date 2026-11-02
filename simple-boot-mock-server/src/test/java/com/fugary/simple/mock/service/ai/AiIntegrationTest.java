package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.mapper.mock.AiConfigMapper;
import com.fugary.simple.mock.mapper.mock.AiLogMapper;
import com.fugary.simple.mock.web.controllers.admin.AiController;
import com.fugary.simple.mock.web.vo.query.SimplePage;
import com.fugary.simple.mock.web.vo.query.AiConfigQueryVo;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;

import java.util.Collections;
import com.fugary.simple.mock.service.impl.ai.AiServiceImpl;
import com.fugary.simple.mock.service.ai.provider.AiChatProvider;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiStreamSession;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
        config.setModifyDate(new java.util.Date(0));
        return controller.save(config).getResultData();
    }

    private AiChatRequest request() {
        AiChatRequest request = new AiChatRequest();
        request.setPrompt("test");
        return request;
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
        assertNotEquals(new java.util.Date(0), configs.selectById(alice.getId()).getModifyDate());
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
        entry.setCreateDate(new java.util.Date(1700000000000L));
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
        query.setStartDate(new java.util.Date(entry.getCreateDate().getTime() + 1));
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
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
