package com.fugary.simple.mock.service.impl.mock;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.push.DefaultScriptWithFetchProviderImpl;
import com.fugary.simple.mock.script.JavaScriptEngineFactory;
import com.fugary.simple.mock.script.JavaScriptEngineProviderImpl;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fugary.simple.mock.contants.MockDiagnoseConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockRequestRoundRobinTest {

    private static final AtomicInteger REQUEST_IDS = new AtomicInteger(1000000);
    private static GraalJSScriptEngine scriptEngine;
    private MockRequestServiceImpl service;
    private JavaScriptEngineProviderImpl provider;
    private MockRequest request;
    private HttpRequestVo requestVo;
    private List<MockData> responses;
    private MockDiagnoseVo diagnose;

    @BeforeAll
    static void createScriptEngine() throws Exception {
        JavaScriptEngineFactory factory = new JavaScriptEngineFactory();
        factory.setScriptWithFetchProvider(new DefaultScriptWithFetchProviderImpl());
        scriptEngine = (GraalJSScriptEngine) factory.create();
    }

    @AfterAll
    static void closeScriptEngine() {
        scriptEngine.close();
    }

    @BeforeEach
    void setUp() {
        provider = new JavaScriptEngineProviderImpl();
        provider.setScriptWithFetchProvider(new DefaultScriptWithFetchProviderImpl());
        service = new MockRequestServiceImpl();
        ReflectionTestUtils.setField(service, "scriptEngineProvider", provider);
        request = new MockRequest();
        request.setId(REQUEST_IDS.incrementAndGet());
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_ROUND_ROBIN);
        request.setRoundRobinKeyExpression("request.body.id");
        requestVo = new HttpRequestVo();
        responses = List.of(new MockData(), new MockData());
        responses.get(0).setId(1);
        responses.get(1).setId(2);
        MockJsUtils.setCurrentScriptEngine(scriptEngine);
        MockJsUtils.setCurrentRequestVo(requestVo);
        diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
    }

    @AfterEach
    void clearScriptContext() {
        MockJsUtils.removeCurrentRequestVo();
        MockJsUtils.removeCurrentScriptEngine();
        MockDiagnoseContext.clear();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldKeepIndependentCountersForInterleavedKeys(boolean fetchEnabled) {
        provider.setFetchEnabled(fetchEnabled);

        assertSame(responses.get(0), next("A"));
        MockDiagnoseVo.Step step = diagnose.getSteps().get(0);
        assertEquals(CODE_ROUND_ROBIN_KEY_RESOLVED, step.getCode());
        assertEquals("request.body.id", step.getDetails().get(KEY_ROUND_ROBIN_KEY_EXPRESSION));
        assertEquals("A", step.getDetails().get(KEY_ROUND_ROBIN_KEY));
        assertEquals(request.getId() + ":\"A\"", step.getDetails().get(KEY_ROUND_ROBIN_COUNTER_KEY));
        assertSame(responses.get(0), next("B"));
        assertSame(responses.get(1), next("A"));
        assertSame(responses.get(1), next("B"));
        assertSame(responses.get(0), next("A"));
    }

    @Test
    void shouldKeepLegacyCounterWhenExpressionIsBlank() {
        request.setRoundRobinKeyExpression(null);
        assertSame(responses.get(0), next("A"));
        request.setRoundRobinKeyExpression("  ");
        assertSame(responses.get(1), next("B"));
        assertSame(responses.get(0), next("C"));
        assertTrue(diagnose.getSteps().isEmpty());
    }

    @Test
    void shouldIsolateRequestsWithTheSameBusinessKey() {
        Integer firstRequestId = request.getId();
        assertSame(responses.get(0), next("A"));
        request.setId(REQUEST_IDS.incrementAndGet());
        assertSame(responses.get(0), next("A"));
        request.setId(firstRequestId);
        assertSame(responses.get(1), next("A"));
    }

    @Test
    void shouldSupportQueryAndPathParameters() {
        request.setRoundRobinKeyExpression("request.params.id");
        requestVo.getParameters().put("id", "A");
        assertSame(responses.get(0), next("ignored"));
        requestVo.getPathParameters().put("id", "B");
        assertSame(responses.get(0), next("ignored"));
        requestVo.getPathParameters().clear();
        assertSame(responses.get(1), next("ignored"));
    }

    @Test
    void shouldAcceptZeroAndDistinguishNumbersFromStrings() {
        assertSame(responses.get(0), next(0));
        assertSame(responses.get(0), next("0"));
        assertSame(responses.get(1), next(0));
        assertSame(responses.get(1), next("0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "undefined", "''", "'  '", "true", "NaN", "Infinity", "[]", "({id: 1})",
            "request.body.id(", "request.body.missing.value"})
    void shouldFallBackToExistingRequestCounterForInvalidKeys(String expression) {
        request.setRoundRobinKeyExpression(null);
        assertSame(responses.get(0), next("A"));
        request.setRoundRobinKeyExpression(expression);
        assertSame(responses.get(1), next("B"));
        MockDiagnoseVo.Step step = diagnose.getSteps().get(0);
        assertEquals(CODE_ROUND_ROBIN_KEY_FALLBACK, step.getCode());
        assertEquals(STATUS_WARNING, step.getStatus());
        assertEquals(request.getId().toString(), step.getDetails().get(KEY_ROUND_ROBIN_COUNTER_KEY));
        assertTrue(step.getDetails().containsKey(KEY_ROUND_ROBIN_FALLBACK_REASON));
        assertSame(responses.get(0), next("C"));
        request.setRoundRobinKeyExpression("request.body.id");
        assertSame(responses.get(0), next("A"));
    }

    @Test
    void shouldRecordScriptErrorDetailsWithoutFailingResponse() {
        request.setRoundRobinKeyExpression("request.body.missing.value");
        assertSame(responses.get(0), next("A"));
        Map<String, Object> details = diagnose.getSteps().get(0).getDetails();
        assertEquals(ROUND_ROBIN_EXPRESSION_ERROR, details.get(KEY_ROUND_ROBIN_FALLBACK_REASON));
        assertTrue(details.get(KEY_MESSAGE).toString().contains("TypeError"));
    }

    @Test
    void shouldOnlyEvaluateExpressionWhenRoundRobinIsNeeded() {
        request.setRoundRobinKeyExpression("request.body.missing.value");
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_AUTO);
        assertSame(responses.get(0), next("A"));
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_RANDOM);
        MockData randomResponse = next("A");
        assertTrue(randomResponse == responses.get(0) || randomResponse == responses.get(1));
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_ROUND_ROBIN);
        assertSame(responses.get(0), service.findMockData(request, List.of(responses.get(0))));
        assertNull(service.findMockData(request, Collections.emptyList()));
        responses.get(0).setMatchPattern("true");
        responses.get(1).setMatchPattern("true");
        assertSame(responses.get(0), service.findMockDataByRequest(responses, requestVo));
        assertSame(responses.get(0), service.findForceMockData(responses, 1));
    }

    private MockData next(Object id) {
        requestVo.setBody(Map.of("id", id));
        return service.findMockData(request, responses);
    }
}
