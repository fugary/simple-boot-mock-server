package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
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
            mockData.setDefaultFlag(1);
            return this.saveOrUpdate(mockData);
        }
        return result;
    }

    /**
     * 取消其他的默认标记
     *
     * @param mockData
     */
    protected void unMarkOtherDefault(MockData mockData) {
        List<MockData> defaultList = list(Wrappers.<MockData>query().eq("request_id", mockData.getRequestId())
                .isNull("modify_from").eq("default_flag", 1)
                .ne("id", mockData.getId())); //  获取default_flag为1的数据
        defaultList.forEach(md -> {
            md.setDefaultFlag(0);
            SimpleMockUtils.addAuditInfo(md);
            saveOrUpdate(md);
        });
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
        this.remove(Wrappers.<MockData>query().eq("modify_from", id));
        return removeById(id);
    }

    @Override
    public boolean deleteMockDatas(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return true;
        }
        mockSchemaService.remove(Wrappers.<MockSchema>query().in("data_id", ids));
        this.remove(Wrappers.<MockData>query().in("modify_from", ids));
        return removeByIds(ids);
    }

    @Override
    public boolean saveOrUpdate(MockData entity) {
        if (entity.getId() == null || entity.getVersion() == null) {
            entity.setVersion(1);
        }
        boolean needSave = true;
        if (entity.getId() != null) {
            MockData historyData = getById(entity.getId());
            if (historyData != null) {
                needSave = !SimpleMockUtils.isSameMockData(historyData, entity);
                if (needSave) {
                    historyData.setId(null);
                    historyData.setModifyFrom(entity.getId());
                    historyData.setVersion(entity.getVersion());
                    if (StringUtils.isBlank(historyData.getModifier())) {
                        historyData.setModifier(historyData.getCreator());
                    }
                    if (historyData.getModifyDate() == null) {
                        historyData.setModifyDate(historyData.getCreateDate());
                    }
                    this.save(historyData);
                }
            }
        }
        boolean result = true;
        if (needSave) {
            result = super.saveOrUpdate(entity);
            if(result && SimpleMockUtils.isDefault(entity)){
                unMarkOtherDefault(entity);
            }
        }
        return result;
    }
}
