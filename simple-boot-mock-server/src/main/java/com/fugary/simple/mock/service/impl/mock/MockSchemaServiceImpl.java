package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.mapper.mock.MockSchemaMapper;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockSchemaServiceImpl extends ServiceImpl<MockSchemaMapper, MockSchema> implements MockSchemaService {

    @Override
    public void saveCopySchemas(Map<String, List<MockSchema>> schemaMap, Integer oldRequestId, Integer oldDataId, Integer groupId, Integer requestId, Integer dataId) {
        List<MockSchema> schemas = schemaMap.get(StringUtils.join(oldRequestId, "-", oldDataId));
        this.saveCopySchemas(schemas, groupId, requestId, dataId);
    }

    @Override
    public void saveCopySchemas(List<MockSchema> schemas, Integer groupId, Integer requestId, Integer dataId) {
        if (schemas != null) {
            schemas.forEach(schema -> {
                schema.setId(null);
                schema.setGroupId(groupId);
                schema.setRequestId(requestId);
                schema.setDataId(dataId);
            });
            this.saveOrUpdateBatch(schemas);
        }
    }

    @Override
    public List<MockSchema> querySchemas(Integer requestId, Integer dataId) {
        if (requestId != null) {
            return this.lambdaQuery().eq(MockSchema::getRequestId, requestId)
                    .eq(dataId != null, MockSchema::getDataId, dataId)
                    .isNull(dataId == null, MockSchema::getDataId)
                    .list();
        }
        return List.of();
    }

    @Override
    public List<MockSchema> queryGroupSchemas(Integer groupId) {
        return this.lambdaQuery().eq(MockSchema::getGroupId, groupId)
                .isNull(MockSchema::getRequestId)
                .list();
    }
}
