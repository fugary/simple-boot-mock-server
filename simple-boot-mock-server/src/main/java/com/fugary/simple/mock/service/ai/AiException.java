package com.fugary.simple.mock.service.ai;

import lombok.Getter;

@Getter
public class AiException extends RuntimeException {
    private final int code;
    private final String messageKey;
    private final Object[] arguments;

    public AiException(int code, String messageKey, Object... arguments) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.arguments = arguments;
    }
}
