package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockProject;

/**
 * Create date 2024/7/15<br>
 *
 * @author gary.fu
 */
public interface MockProjectService extends IService<MockProject> {

    /**
     * 级联删除分组、请求和数据
     *
     * @param id
     * @return
     */
    boolean deleteMockProject(Integer id);

    /**
     * 检查是否有重复
     *
     * @param project
     * @return
     */
    boolean existsMockProject(MockProject project);

}
