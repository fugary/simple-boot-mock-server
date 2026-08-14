package com.fugary.simple.mock;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.exception.GlobalExceptionHandler;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private SimpleMockConfigProperties configProperties;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        configProperties = new SimpleMockConfigProperties();
        configProperties.setMaxUploadSize(10 * 1024 * 1024);
        ReflectionTestUtils.setField(exceptionHandler, "simpleMockConfigProperties", configProperties);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        SimpleResultUtils utils = new SimpleResultUtils();
        utils.injectMessageSource(messageSource);
    }

    @AfterEach
    void tearDown() {
        new SimpleResultUtils().injectMessageSource(null);
    }

    @Test
    void testUploadSizeExceededHandler() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10 * 1024 * 1024);
        SimpleResult<Object> result = exceptionHandler.uploadSizeExceededHandler(ex);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(MockErrorConstants.CODE_2005, result.getCode());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("10MB"));
    }

    @Test
    void testGenericExceptionHandler() {
        RuntimeException ex = new RuntimeException("Something failed");
        SimpleResult<Object> result = exceptionHandler.exceptionHandler(ex);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(MockErrorConstants.CODE_500, result.getCode());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals("RuntimeException: Something failed", result.getMessage());
    }
}
