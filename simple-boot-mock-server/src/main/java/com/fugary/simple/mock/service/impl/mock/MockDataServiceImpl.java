package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.mapper.mock.MockDataMapper;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.service.mock.MockDataService;
import org.springframework.stereotype.Service;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockDataServiceImpl extends ServiceImpl<MockDataMapper, MockData> implements MockDataService {

    @Override
    public boolean markMockDataDefault(MockData mockData) {
        MockData existMockData = getById(mockData.getId());
        boolean result = false;
        if (existMockData != null && existMockData.getRequestId().equals(mockData.getRequestId())) {
            result = update(Wrappers.<MockData>update()
                    .eq("request_id", existMockData.getRequestId())
                    .set("default_flag", 0));
            if (SimpleMockUtils.isDefault(mockData)) {
                result = update(Wrappers.<MockData>update()
                        .eq("id", existMockData.getId())
                        .set("default_flag", 1));
            }
        }
        return result;
    }

    @Override
    public boolean saveOrUpdate(MockData entity) {
        boolean result = super.saveOrUpdate(entity);
        if(result && SimpleMockUtils.isDefault(entity)){
            this.markMockDataDefault(entity);
        }
        return result;
    }
}
