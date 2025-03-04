package com.fugary.simple.mock.interceptors;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.events.OperationLogEvent;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleLogUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.controllers.MockController;
import com.fugary.simple.mock.web.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Create date 2024/11/14<br>
 *
 * @author gary.fu
 */
@Component
@Aspect
@Slf4j
public class CrudOperationLogInterceptor implements ApplicationContextAware {

    @Autowired
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @Autowired
    private MockGroupService mockGroupService;

    private ApplicationContext applicationContext;

    @Pointcut("execution(* com..controllers..*.*(..))")
    public void crudOperation() {
    }

    @Around("crudOperation()")
    public Object proceedingMethod(ProceedingJoinPoint joinpoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;
        boolean mockLogEnabled = simpleMockConfigProperties.isMockLogEnabled();
        MockLog.MockLogBuilder logBuilder = mockLogEnabled ? initLogBuilder(joinpoint) : null;
        try {
            SimpleLogUtils.setLogBuilder(logBuilder);
            result = joinpoint.proceed();
        } catch (Throwable e) {
            exception = e;
        } finally {
            SimpleLogUtils.clearLogBuilder();
        }
        if (mockLogEnabled) {
            processLog(logBuilder, joinpoint, startTime, result, exception);
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    protected MockLog.MockLogBuilder initLogBuilder(ProceedingJoinPoint joinpoint) {
        HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
        MockLog.MockLogBuilder logBuilder = null;
        if (request != null) {
            String groupPath = mockGroupService.calcGroupPath(request.getServletPath());
            if (checkNeedLog(joinpoint)) {
                logBuilder = MockLog.builder()
                        .ipAddress(HttpRequestUtils.getIp(request))
                        .logType(request.getMethod())
                        .headers(JsonUtils.toJson(HttpRequestUtils.getRequestHeadersMap(request)))
                        .mockGroupPath(groupPath)
                        .requestUrl(HttpRequestUtils.getRequestUrl(request));
            }
        }
        return logBuilder;
    }

    private boolean checkNeedLog(ProceedingJoinPoint joinpoint) {
        HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
        Class<?> declaringType = joinpoint.getSignature().getDeclaringType();
        return request != null && (MockController.class.isAssignableFrom(declaringType) || !HttpMethod.GET.matches(request.getMethod()));
    }

    private void processLog(MockLog.MockLogBuilder logBuilder, ProceedingJoinPoint joinpoint, long startTime, Object result, Throwable exception) {
        MethodSignature signature = (MethodSignature) joinpoint.getSignature();
        Object[] args = joinpoint.getArgs();
        if (logBuilder != null) {
            String logName = getLogName(signature);
            MockUser loginUser = SecurityUtils.getLoginUser();
            Date createDate = new Date();
            logBuilder.logName(logName)
                    .createDate(createDate)
                    .logTime(createDate.getTime() - startTime)
                    .exceptions(exception == null ? null : ExceptionUtils.getStackTrace(exception));
            if (loginUser != null) {
                logBuilder.userName(loginUser.getUserName())
                        .creator(loginUser.getUserName());
            }
            boolean success = exception == null;
            if (result instanceof SimpleResult) {
                SimpleResult<?> simpleResult = ((SimpleResult<?>) result);
                logBuilder.logMessage(simpleResult.getMessage());
                success = simpleResult.isSuccess();
                logBuilder.responseBody(JsonUtils.toJson(simpleResult));
            }
            logBuilder.logResult(success ? MockConstants.SUCCESS : MockConstants.FAIL);
            Pair<Boolean, MockUser> loginPair = checkLogin(logName, args);
            if (loginPair.getLeft()) {
                MockUser loginVo = loginPair.getRight();
                logBuilder.userName(loginVo.getUserName());
                if (success) {
                    logBuilder.creator(loginVo.getUserName());
                }
                logBuilder.logData(SimpleMockUtils.logDataString(List.of(loginVo)));
            } else {
                List<Object> argsList = Arrays.stream(args).filter(this::isValidParam).collect(Collectors.toList());
                logBuilder.logData(SimpleMockUtils.logDataString(argsList));
            }
            HttpServletResponse response = HttpRequestUtils.getCurrentResponse();
            if (response != null) {
                String header = response.getHeader(MockConstants.MOCK_DATA_ID_HEADER);
                if (StringUtils.isNotBlank(header)) {
                    logBuilder.dataId(header);
                }
            }
            MockLog mockLog = logBuilder.build();
            publishEvent(mockLog);
        }
    }

    private boolean isValidParam(Object target) {
        return target != null && (target.getClass().isPrimitive() || target instanceof Serializable);
    }

    private Pair<Boolean, MockUser> checkLogin(String logName, Object[] args) {
        boolean isLogin = false;
        if ("LoginController#login".equals(logName)) {
            isLogin = true;
            Object loginVo = Arrays.stream(args).filter(arg -> arg instanceof MockUser).findFirst().orElse(null);
            return Pair.of(isLogin, (MockUser) loginVo);
        }
        return Pair.of(isLogin, null);
    }

    private String getLogName(MethodSignature signature) {
        Class<?> declaringType = signature.getDeclaringType();
        Method method = signature.getMethod();
        String methodName = method.getName();
        return declaringType.getSimpleName() + "#" + methodName;
    }

    private void publishEvent(MockLog mockLog) {
        applicationContext.publishEvent(new OperationLogEvent(mockLog));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
