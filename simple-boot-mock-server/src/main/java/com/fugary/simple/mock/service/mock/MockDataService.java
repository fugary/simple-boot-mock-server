package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;

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
}
