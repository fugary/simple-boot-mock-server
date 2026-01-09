package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.web.vo.http.HttpResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Create date 2026/1/9<br>
 *
 * @author gary.fu
 */
@Component
public class DefaultMockPostScriptProcessorImpl implements MockPostScriptProcessor {

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

    @Override
    public String process(MockRequest mockRequest, MockData mockData, String responseBody) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, mockData);
        if (StringUtils.isNotBlank(postProcessor)) {
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(mockData.getStatusCode());
            responseVo.setBodyStr(responseBody);
            responseVo.setBody(MockJsUtils.getObjectBody(responseVo.getBodyStr()));
            MockJsUtils.setCurrentResponseVo(responseVo);
            responseBody = scriptEngineProvider.evalStr("mockStringify(" + MockJsUtils.getJsExpression(postProcessor) + ")");
        }
        return responseBody;
    }

    @Override
    public ResponseEntity<?> process(MockRequest mockRequest, ResponseEntity<?> responseEntity) {
        String postProcessor = SimpleMockUtils.getPostProcessor(mockRequest, null);
        if (StringUtils.isNotBlank(postProcessor)) {
            HttpResponseVo responseVo = new HttpResponseVo();
            responseVo.setStatusCode(responseEntity.getStatusCode().value());
            Object body = responseEntity.getBody();
            String responseBody = StringUtils.EMPTY;
            if (body instanceof byte[]) {
                responseBody = new String((byte[]) body, StandardCharsets.UTF_8);
            } else if (body instanceof String) {
                responseBody = (String) body;
            }
            responseVo.setBodyStr(responseBody);
            responseVo.setBody(MockJsUtils.getObjectBody(responseVo.getBodyStr()));
            MockJsUtils.setCurrentResponseVo(responseVo);
            responseBody = scriptEngineProvider.evalStr("mockStringify(" + MockJsUtils.getJsExpression(postProcessor) + ")");
            // 修改 body
            byte[] bodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            responseEntity = ResponseEntity
                    .status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
                    .contentLength(bodyBytes.length)
                    .body(bodyBytes);
        }
        return responseEntity;
    }

}
