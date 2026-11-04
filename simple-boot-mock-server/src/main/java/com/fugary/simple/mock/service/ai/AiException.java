package com.fugary.simple.mock.service.ai;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class AiException extends RuntimeException {
    private final int code;
    private final String messageKey;
    private final Object[] arguments;
    private String detail;

    public AiException(int code, String messageKey, Object... arguments) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.arguments = arguments;
    }

    public AiException withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    public String getErrorMessage() {
        String message = SimpleResultUtils.getErrorMsg(messageKey, arguments);
        return StringUtils.isBlank(detail) ? message : message + "\n" + detail;
    }
}
