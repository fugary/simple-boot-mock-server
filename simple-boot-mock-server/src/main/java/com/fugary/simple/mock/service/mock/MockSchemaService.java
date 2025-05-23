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
     * @param groupId
     * @param requestId
     * @param dataId
     */
    void saveCopySchemas(Map<String, List<MockSchema>> schemaMap, Integer oldRequestId, Integer oldDataId,
                         Integer groupId, Integer requestId, Integer dataId);

    /**
     * 复制时写入schema信息
     *
     * @param schemas
     * @param groupId
     * @param requestId
     * @param dataId
     */
    void saveCopySchemas(List<MockSchema> schemas, Integer groupId, Integer requestId, Integer dataId);

    /**
     * 查询Schema列表
     * @param requestId
     * @param dataId
     * @return
     */
    List<MockSchema> querySchemas(Integer requestId, Integer dataId);

    /**
     * 查询Schema列表
     * @param groupId
     * @return
     */
    List<MockSchema> queryGroupSchemas(Integer groupId);
}
