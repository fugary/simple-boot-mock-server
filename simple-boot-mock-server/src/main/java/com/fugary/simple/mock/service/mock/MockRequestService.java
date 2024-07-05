package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;

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

    /**
     * 根据请求获取对应的响应数据，优先取默认响应或者第一条响应
     *
     * @param request
     * @param defaultId
     * @return
     */
    MockData findMockData(MockRequest request, Integer defaultId);

    /**
     * 根据请求获取对应的响应数据，优先取默认响应或者第一条响应
     *
     * @param requestId
     * @param defaultId
     * @return
     */
    MockData findMockData(Integer requestId, Integer defaultId);

    /**
     * 把请求相关参数保存到数据库
     *
     * @param mockData
     * @return
     */
    boolean saveMockParams(MockData mockData);
}
