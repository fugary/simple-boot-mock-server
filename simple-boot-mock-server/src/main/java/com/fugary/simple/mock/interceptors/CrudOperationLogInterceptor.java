package com.fugary.simple.mock.interceptors;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.events.OperationLogEvent;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.SimpleLogUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.controllers.MockController;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
                        .requestUrl(HttpRequestUtils.getRequestUrl(request))
                        .diagnoseId(SimpleMockUtils.isMockPreview(request)
                                ? SimpleMockUtils.getOrCreateMockDiagnoseId(request) : null)
                        .extend1(request.getHeader("mock_uuid"));
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
            long logTime = createDate.getTime() - startTime;
            logBuilder.logName(logName)
                    .createDate(createDate)
                    .logTime(logTime)
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
                List<Object> argsList = Arrays.stream(args).map(this::processRequestBody).filter(this::isValidParam).collect(Collectors.toList());
                logBuilder.logData(SimpleMockUtils.logDataString(argsList));
            }
            HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
            HttpServletResponse response = HttpRequestUtils.getCurrentResponse();
            Map<String, String> responseHeaders = new LinkedHashMap<>();
            if (request != null && response != null) {
                responseHeaders = getResponseHeaders(response, result);
                String header = getHeader(responseHeaders, MockConstants.MOCK_DATA_ID_HEADER);
                String userName = StringUtils.defaultIfBlank(getHeader(responseHeaders, MockConstants.MOCK_DATA_USER_HEADER),
                        request.getHeader(MockConstants.MOCK_DATA_USER_HEADER));
                if (StringUtils.isNotBlank(header)) {
                    logBuilder.dataId(header);
                }
                if (StringUtils.isNotBlank(userName)) {
                    logBuilder.userName(userName).creator(userName);
                }
                String proxyUrl = getHeader(responseHeaders, MockConstants.MOCK_PROXY_URL_HEADER);
                if (StringUtils.isNotBlank(proxyUrl)) {
                    logBuilder.proxyUrl(proxyUrl);
                }
                logBuilder.responseHeaders(JsonUtils.toJson(responseHeaders));
            }
            Integer responseStatusCode = getResponseStatus(response, result);
            String responseContentType = getResponseContentType(response, result, responseHeaders);
            if (responseStatusCode != null) {
                logBuilder.responseStatusCode(responseStatusCode);
            }
            if (StringUtils.isNotBlank(responseContentType)) {
                logBuilder.responseContentType(responseContentType);
            }
            MockLog mockLog = logBuilder.build();
            completeDiagnoseInfo(mockLog, responseStatusCode, responseContentType, logTime);
            publishEvent(mockLog);
        }
    }

    private void completeDiagnoseInfo(MockLog mockLog, Integer responseStatusCode, String responseContentType,
                                      long logTime) {
        MockDiagnoseVo diagnose = MockDiagnoseContext.get();
        if (mockLog == null || diagnose == null || diagnose.getSteps().isEmpty()) {
            return;
        }
        diagnose.completeHttpInfo(responseStatusCode, responseContentType, logTime);
        mockLog.setDiagnoseData(JsonUtils.toJson(diagnose));
    }

    private Integer getResponseStatus(HttpServletResponse response, Object result) {
        if (result instanceof ResponseEntity) {
            return ((ResponseEntity<?>) result).getStatusCodeValue();
        }
        return response == null ? null : response.getStatus();
    }

    private String getResponseContentType(HttpServletResponse response, Object result, Map<String, String> responseHeaders) {
        if (result instanceof ResponseEntity) {
            MediaType mediaType = ((ResponseEntity<?>) result).getHeaders().getContentType();
            if (mediaType != null) {
                return mediaType.toString();
            }
        }
        String contentType = responseHeaders == null ? null : getHeader(responseHeaders, HttpHeaders.CONTENT_TYPE);
        return StringUtils.defaultIfBlank(contentType, response == null ? null : response.getContentType());
    }

    private Object processRequestBody(Object arg) {
        if (arg instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper contentCachingRequestWrapper = (ContentCachingRequestWrapper) arg;
            if (contentCachingRequestWrapper.getContentAsByteArray().length > 0) {
                return new String(contentCachingRequestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            }
        }
        return arg;
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

    private Map<String, String> getResponseHeaders(HttpServletResponse response, Object result) {
        Map<String, String> responseHeaders = new LinkedHashMap<>();
        HttpRequestUtils.getResponseHeadersMap(response).forEach((headerName, headerValue) ->
                putHeader(responseHeaders, headerName, headerValue));
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            responseEntity.getHeaders().forEach((headerName, values) ->
                    putHeader(responseHeaders, headerName, StringUtils.join(values, ",")));
        }
        removeHeader(responseHeaders, MockConstants.MOCK_DIAGNOSE_ID_HEADER);
        return responseHeaders;
    }

    private String getHeader(Map<String, String> responseHeaders, String headerName) {
        return responseHeaders.entrySet().stream()
                .filter(entry -> headerName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private void removeHeader(Map<String, String> responseHeaders, String headerName) {
        responseHeaders.keySet().stream()
                .filter(headerName::equalsIgnoreCase)
                .findFirst()
                .ifPresent(responseHeaders::remove);
    }

    private void putHeader(Map<String, String> responseHeaders, String headerName, String headerValue) {
        if (StringUtils.isBlank(headerName)) {
            return;
        }
        String existsHeader = responseHeaders.keySet().stream()
                .filter(headerName::equalsIgnoreCase)
                .findFirst()
                .orElse(null);
        if (existsHeader != null) {
            responseHeaders.remove(existsHeader);
        }
        responseHeaders.put(headerName, headerValue);
    }

    private void publishEvent(MockLog mockLog) {
        applicationContext.publishEvent(new OperationLogEvent(mockLog));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
