package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.web.vo.SimpleResult;

import java.util.List;

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
     * 级联删除分组、请求和数据
     *
     * @param ids
     * @return
     */
    boolean deleteMockProjects(List<Integer> ids);

    /**
     * 检查是否有重复
     *
     * @param project
     * @return
     */
    boolean existsMockProject(MockProject project);

    /**
     * 加载项目
     * @param userName
     * @param projectCode
     * @return
     */
    MockProject loadMockProject(String userName, String projectCode);

    /**
     * 检查project是否正常
     *
     * @param userName
     * @param projectCode
     * @return
     */
    boolean checkProjectValid(String userName, String projectCode);

    /**
     * 复制一份数据
     *
     * @param projectId
     * @return
     */
    SimpleResult<MockProject> copyMockProject(Integer projectId);

    /**
     * 保存项目信息
     *
     * @param project
     * @return
     */
    SimpleResult<MockProject> saveMockProject(MockProject project);
}
