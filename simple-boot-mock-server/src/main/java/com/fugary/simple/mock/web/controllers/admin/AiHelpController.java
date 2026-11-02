package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiHelpService;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.service.ai.provider.AiSseSupport;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/admin/ai/help")
public class AiHelpController {
    private final AiHelpService help;

    @Autowired
    private AiSseSupport sse;

    public AiHelpController(AiHelpService help) {
        this.help = help;
    }

    @GetMapping
    public SimpleResult<Boolean> available() {
        return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_0, help.available());
    }

    @PostMapping
    public SimpleResult<AiChatResponse> ask(@RequestBody AiChatRequest question) {
        return SimpleResultUtils.createSimpleResult(help.ask(question));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestBody AiChatRequest question) {
        return sse.start((emitter, stream) -> {
            AiChatResponse response = help.ask(question, stream);
            AiSseSupport.send(emitter, "finish",
                Map.of("status", "SUCCESS", "resultData", response));
        });
    }

    @ExceptionHandler(AiException.class)
    public SimpleResult<Void> handleError(AiException exception) {
        return SimpleResultUtils.createError(exception.getCode(),
            SimpleResultUtils.getErrorMsg(exception.getMessageKey(), exception.getArguments()));
    }
}
