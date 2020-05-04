package com.mengstudy.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengstudy.simple.mock.entity.mock.MockRequest;

/**
 * Created on 2020/5/3 22:36 .<br>
 *
 * @author gary.fu
 */
public interface MockRequestService extends IService<MockRequest> {
    /**
     * 删除配置的请求
     *
     * @param requestId
     * @return
     */
    boolean deleteMockRequest(Integer requestId);

    /**
     * 判断是否已经存在改Request
     *
     * @param request
     * @return
     */
    boolean existsMockRequest(MockRequest request);
}
