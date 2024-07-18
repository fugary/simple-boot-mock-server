package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;

import java.util.List;

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
     * 复制一份数据
     *
     * @param requestId
     * @return
     */
    boolean copyMockRequest(Integer requestId);

    /**
     * 根据请求获取对应的响应数据，优先取默认响应或者第一条响应
     *
     * @param requestId
     * @param defaultId
     * @return
     */
    MockData findMockData(Integer requestId, Integer defaultId);

    /**
     * 加载请求下面的可用数据
     * @param requestId
     */
    List<MockData> loadDataByRequest(Integer requestId);

    /**
     * 查询默认可用MockData
     *
     * @param mockDataList
     * @return
     */
    MockData findMockData(List<MockData> mockDataList);

    /**
     * 查找指定的MockData
     *
     * @param mockDataList
     * @param defaultId
     * @return
     */
    MockData findForceMockData(List<MockData> mockDataList, Integer defaultId);

    /**
     * 查找匹配规则的MockData
     *
     * @param mockDataList
     * @param requestVo
     * @return
     */
    MockData findMockDataByRequest(List<MockData> mockDataList, HttpRequestVo requestVo);

    /**
     * 把请求相关参数保存到数据库
     *
     * @param mockData
     * @return
     */
    boolean saveMockParams(MockData mockData);
}
