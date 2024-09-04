package com.fugary.simple.mock.exception;

import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Created by gary.fu on 2024/8/20.
 */
@Component
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ResponseBody
	@ExceptionHandler({Exception.class})
	public <T> SimpleResult<T> exceptionHandler(Exception e) {
		return SimpleResultUtils.createError(ExceptionUtils.getMessage(e));
	}
}
