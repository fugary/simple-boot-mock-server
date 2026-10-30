package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.service.ai.AiHelpService;
import com.fugary.simple.mock.service.ai.provider.AiChatRequest;
import com.fugary.simple.mock.service.ai.provider.AiChatResponse;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ai/help")
public class AiHelpController {
    private final AiHelpService help;

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

    @ExceptionHandler(AiException.class)
    public SimpleResult<Void> handleError(AiException exception) {
        return SimpleResultUtils.createError(exception.getCode(),
            SimpleResultUtils.getErrorMsg(exception.getMessageKey(), exception.getArguments()));
    }
}
