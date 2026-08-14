package com.fugary.simple.mock.exception;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/**
 * Created by gary.fu on 2024/8/20.
 */
@Slf4j
@Component
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired(required = false)
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @ResponseBody
    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public <T> SimpleResult<T> uploadSizeExceededHandler(Exception e) {
        log.warn("Upload size exceeded error: {}", e.getMessage());
        long maxSize = -1;
        if (e instanceof MaxUploadSizeExceededException) {
            maxSize = ((MaxUploadSizeExceededException) e).getMaxUploadSize();
        }
        if (maxSize <= 0 && simpleMockConfigProperties != null) {
            maxSize = simpleMockConfigProperties.getMaxUploadSize();
        }
        String maxSizeStr = maxSize > 0 ? SimpleMockUtils.formatFileSize(maxSize) : "10MB";
        String message = SimpleResultUtils.getErrorMsg("simple.error.code." + MockErrorConstants.CODE_2005, new Object[]{maxSizeStr});
        return SimpleResultUtils.createError(MockErrorConstants.CODE_2005, message);
    }

    @ResponseBody
    @ExceptionHandler({Exception.class})
    public <T> SimpleResult<T> exceptionHandler(Exception e) {
        log.error("Global internal error: ", e);
        return SimpleResultUtils.createError(ExceptionUtils.getMessage(e));
    }
}
