package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;

import java.util.List;

/**
 * Created on 2020/5/3 22:36 .<br>
 *
 * @author gary.fu
 */
public interface MockDataService extends IService<MockData> {

    /**
     * 标记为default
     *
     * @param mockData
     * @return
     */
    boolean markMockDataDefault(MockData mockData);

    /**
     * 复制一份数据
     *
     * @param dataId
     * @return
     */
    boolean copyMockData(Integer dataId);

    /**
     * 删除mock数据
     *
     * @param id
     * @return
     */
    boolean deleteMockData(Integer id);

    /**
     * 删除mock数据
     *
     * @param ids
     * @return
     */
    boolean deleteMockDatas(List<Integer> ids);
}
