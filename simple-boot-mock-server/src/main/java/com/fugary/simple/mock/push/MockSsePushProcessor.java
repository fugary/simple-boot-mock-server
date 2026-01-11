package com.fugary.simple.mock.push;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 代理处理器接口
 * 专门处理 Server-Sent Events (SSE) 类型的代理请求
 * 
 * Create date 2026/01/03<br>
 *
 * @author gary.fu
 */
public interface MockSsePushProcessor {

    /**
     * 处理 SSE 代理请求
     *
     * @param mockParams  代理请求参数
     * @param mockRequest mock请求
     * @param mockData    mock数据
     * @return SseEmitter 用于流式传输 SSE 事件
     */
    SseEmitter processSseProxy(MockParamsVo mockParams, MockRequest mockRequest, MockData mockData);
}
