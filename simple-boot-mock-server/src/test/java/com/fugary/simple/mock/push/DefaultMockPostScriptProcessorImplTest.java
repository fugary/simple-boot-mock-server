package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.impl.mock.MockDiagnoseRecorder;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.MockDiagnoseContext;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.web.vo.http.HttpResponseVo;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMockPostScriptProcessorImplTest {

    @AfterEach
    void tearDown() {
        MockDiagnoseContext.clear();
        MockJsUtils.removeCurrentResponseVo();
    }

    @Test
    void processShouldRecordPostProcessorAroundExternalFetchDiagnose() {
        MockDiagnoseVo diagnose = new MockDiagnoseVo();
        MockDiagnoseContext.set(diagnose);
        DefaultMockPostScriptProcessorImpl processor = new DefaultMockPostScriptProcessorImpl();
        ReflectionTestUtils.setField(processor, "scriptEngineProvider", new FetchDiagnoseScriptEngineProvider());

        MockRequest request = new MockRequest();
        request.setPostProcessor("(async function () { return response.body; }())");
        MockData data = new MockData();
        data.setStatusCode(200);
        data.setContentType("text/plain");

        String responseBody = processor.process(request, data, "{\"before\":true}");
        diagnose.finish("mock", "mock_return");

        assertEquals("{\"ok\":true}", responseBody);
        assertEquals(201, data.getStatusCode());
        assertEquals("application/json", data.getContentType());
        int postStartIndex = indexOfStepCode(diagnose, "post_processor_start");
        int fetchIndex = indexOfStepCode(diagnose, "fetch_return");
        int postReturnIndex = indexOfStepCode(diagnose, "post_processor_return");
        int resultIndex = indexOfStepCode(diagnose, "mock_return");
        assertTrue(postStartIndex >= 0);
        assertTrue(fetchIndex > postStartIndex);
        assertTrue(postReturnIndex > fetchIndex);
        assertTrue(resultIndex > postReturnIndex);
        assertNull(MockJsUtils.getCurrentResponseVo());
    }

    private int indexOfStepCode(MockDiagnoseVo diagnose, String code) {
        for (int i = 0; i < diagnose.getSteps().size(); i++) {
            if (code.equals(diagnose.getSteps().get(i).getCode())) {
                return i;
            }
        }
        return -1;
    }

    private static class FetchDiagnoseScriptEngineProvider implements ScriptEngineProvider {

        @Override
        public String mock(String template) {
            return template;
        }

        @Override
        public Object eval(String script) {
            MockDiagnoseRecorder.of(MockDiagnoseContext.get())
                    .externalFetch("GET", "https://example.com/post", 200,
                            "application/json", 12L, null);
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(201);
            responseVo.getHeaders().put(HttpHeaders.CONTENT_TYPE, "application/json");
            Map<String, String> result = new LinkedHashMap<>();
            result.put("bodyStr", "{\"ok\":true}");
            result.put("responseStr", JsonUtils.toJson(responseVo));
            return JsonUtils.toJson(result);
        }

        @Override
        public String evalStr(String script) {
            return String.valueOf(eval(script));
        }
    }
}
