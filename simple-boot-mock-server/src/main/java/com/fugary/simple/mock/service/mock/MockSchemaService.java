package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockSchema;

import java.util.List;
import java.util.Map;

/**
 * Created on 2024/7/31 11:42 .<br>
 *
 * @author gary.fu
 */
public interface MockSchemaService extends IService<MockSchema> {

    /**
     * 复制时写入schema信息
     *
     * @param schemaMap
     * @param oldRequestId
     * @param oldDataId
     * @param requestId
     * @param dataId
     */
    void saveCopySchemas(Map<String, List<MockSchema>> schemaMap, Integer oldRequestId, Integer oldDataId,
                         Integer requestId, Integer dataId);

    /**
     * 复制时写入schema信息
     *
     * @param schemas
     * @param requestId
     * @param dataId
     */
    void saveCopySchemas(List<MockSchema> schemas, Integer requestId, Integer dataId);

}
