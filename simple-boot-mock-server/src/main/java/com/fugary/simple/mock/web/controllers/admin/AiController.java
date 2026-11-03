package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.service.ai.AiConfigService;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiLogService;
import com.fugary.simple.mock.service.ai.AiService;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiSseSupport;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.AiConfigOption;
import com.fugary.simple.mock.web.vo.AiConfigTestRequest;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.AiConfigQueryVo;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ai")
public class AiController {
    private final AiConfigService configs;
    private final AiLogService logs;
    private final AiService ai;

    @Autowired
    private AiSseSupport sse;

    public AiController(AiConfigService configs, AiLogService logs, AiService ai) {
        this.configs = configs;
        this.logs = logs;
        this.ai = ai;
    }

    @ExceptionHandler(AiException.class)
    public SimpleResult<Void> handleError(AiException exception) {
        return SimpleResultUtils.createError(exception.getCode(),
            SimpleResultUtils.getErrorMsg(exception.getMessageKey(), exception.getArguments()));
    }

    @GetMapping("/configs")
    public SimpleResult<List<AiConfig>> searchConfigs(AiConfigQueryVo query) {
        return SimpleResultUtils.createSimpleResult(configs.search(query));
    }

    @GetMapping("/configs/{id}")
    public SimpleResult<AiConfig> getConfig(@PathVariable Integer id) {
        AiConfig config = configs.getConfig(id);
        if (!SecurityUtils.isAdminUser()) {
            AiConfig visible = new AiConfig();
            BeanUtils.copyProperties(config, visible);
            visible.setApiKey(null);
            config = visible;
        }
        return SimpleResultUtils.createSimpleResult(config);
    }

    @PostMapping("/configs")
    public SimpleResult<AiConfig> save(@RequestBody AiConfig config) {
        AiConfig saved = configs.saveConfig(config);
        saved.setApiKey(null);
        return SimpleResultUtils.createSimpleResult(saved);
    }

    @DeleteMapping("/configs/{id}")
    public SimpleResult<Boolean> deleteConfig(@PathVariable Integer id) {
        return SimpleResultUtils.createSimpleResult(configs.deleteConfig(id));
    }

    @GetMapping("/logs")
    public SimpleResult<List<AiLog>> searchLogs(AiLogQueryVo query) {
        return SimpleResultUtils.createSimpleResult(logs.search(query));
    }

    @GetMapping("/logs/{id}")
    public SimpleResult<AiLog> getLog(@PathVariable Integer id) {
        return SimpleResultUtils.createSimpleResult(logs.getOwned(id));
    }

    @DeleteMapping("/logs/{id}")
    public SimpleResult<Boolean> deleteLog(@PathVariable Integer id) {
        return SimpleResultUtils.createSimpleResult(logs.deleteOwned(id));
    }

    @PostMapping("/configs/{id}/test")
    public SimpleResult<AiChatResponse> test(@PathVariable Integer id, @RequestBody AiChatRequest request) {
        return SimpleResultUtils.createSimpleResult(ai.test(id, request));
    }

    @PostMapping(value = "/configs/{id}/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTest(
            @PathVariable Integer id, @RequestBody AiChatRequest request) {
        configs.getUsableConfig(id);
        return sse.start((emitter, stream) -> {
            AiChatResponse response = ai.test(id, request, stream);
            AiSseSupport.send(emitter, "finish",
                Map.of("status", "SUCCESS", "resultData", response));
        });
    }

    @PostMapping(value = "/configs/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDraftTest(@RequestBody AiConfigTestRequest request) {
        if (!SecurityUtils.isAdminUser()) {
            throw new AiException(403, "simple.error.code.403");
        }
        return sse.start((emitter, stream) -> {
            AiChatResponse response = ai.testDraft(request.getConfig(), request, stream);
            AiSseSupport.send(emitter, "finish",
                Map.of("status", "SUCCESS", "resultData", response));
        });
    }

    @GetMapping("/configs/{id}/models")
    public SimpleResult<List<String>> loadModels(@PathVariable Integer id) {
        return SimpleResultUtils.createSimpleResult(ai.loadModels(id));
    }

    @PostMapping("/configs/models")
    public SimpleResult<List<String>> loadModels(@RequestBody AiConfig config) {
        return SimpleResultUtils.createSimpleResult(ai.loadModels(config));
    }


    @GetMapping("/configs/options")
    public SimpleResult<List<AiConfigOption>> options() {
        return SimpleResultUtils.createSimpleResult(configs.options());
    }

}
