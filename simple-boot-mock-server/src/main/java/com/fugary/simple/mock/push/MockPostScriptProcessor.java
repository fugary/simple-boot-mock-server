package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import org.springframework.http.ResponseEntity;

/**
 * Create date 2026/1/9<br>
 *
 * @author gary.fu
 */
public interface MockPostScriptProcessor {
    /**
     * 后置处理脚本（Mock数据）
     *
     * @param mockRequest
     * @param mockData
     * @param responseBody
     * @return
     */
    String process(MockRequest mockRequest, MockData mockData, String responseBody);

    /**
     * 后置处理脚本（代理）
     *
     * @param mockRequest
     * @param responseEntity
     * @return
     */
    ResponseEntity<?> process(MockRequest mockRequest, ResponseEntity<?> responseEntity);

}
