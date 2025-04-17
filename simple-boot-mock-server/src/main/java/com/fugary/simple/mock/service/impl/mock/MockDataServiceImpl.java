package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.mapper.mock.MockDataMapper;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockDataServiceImpl extends ServiceImpl<MockDataMapper, MockData> implements MockDataService {

    @Autowired
    private MockSchemaService mockSchemaService;

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
    public boolean copyMockData(Integer dataId) {
        MockData data = getById(dataId);
        if (data != null) {
            Integer oldRequestId = data.getId();
            Integer oldDataId = data.getId();
            data.setId(null);
            boolean saved = saveOrUpdate(data);
            if (saved) {
                List<MockSchema> schemas = mockSchemaService.list(Wrappers.<MockSchema>query()
                        .eq("request_id", oldRequestId)
                        .eq("data_id", oldDataId));
                mockSchemaService.saveCopySchemas(schemas, data.getGroupId(), data.getRequestId(), data.getId());
            }
        }
        return true;
    }

    @Override
    public boolean deleteMockData(Integer id) {
        mockSchemaService.remove(Wrappers.<MockSchema>query().eq("data_id", id));
        return removeById(id);
    }

    @Override
    public boolean deleteMockDatas(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        mockSchemaService.remove(Wrappers.<MockSchema>query().in("data_id", ids));
        return removeByIds(ids);
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
