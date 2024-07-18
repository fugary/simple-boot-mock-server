package com.fugary.simple.mock.push;

import com.fugary.simple.mock.web.vo.query.MockParamsVo;
import org.springframework.http.ResponseEntity;

/**
 * Create date 2024/7/17<br>
 *
 * @author gary.fu
 */
public interface MockPushProcessor {

    /**
     * 推送处理器
     *
     * @param mockParams 请求参数
     * @return
     */
    ResponseEntity<byte[]> doPush(MockParamsVo mockParams);
}
