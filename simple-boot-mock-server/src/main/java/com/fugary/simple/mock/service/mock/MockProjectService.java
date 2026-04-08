package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockRequest;
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

    MockProject loadMockProject(String userName, Integer projectId, String projectCode);

    /**
     * 检查project是否正常
     *
     * @param userName
     * @param projectCode
     * @return
     */
    boolean checkProjectValid(String userName, String projectCode);

    boolean checkProjectValid(String userName, Integer projectId, String projectCode);

    /**
     * 复制一份数据
     *
     * @param projectId
     * @param userName
     * @return
     */
    SimpleResult<MockProject> copyMockProject(Integer projectId, String userName);

    /**
     * 保存项目信息
     *
     * @param project
     * @return
     */
    SimpleResult<MockProject> saveMockProject(MockProject project);

    /**
     * 检查当前用户是否有目标项目的对应权限。
     *
     * @param project 目标项目对象
     * @param authority 需要的权限（例如 readable、writable、deletable）
     * @return 是否有权限
     */
    boolean hasProjectAuthority(MockProject project, String authority);

    /**
     * 检查当前用户是否有目标项目的对应权限。
     *
     * @param targetUserName 目标项目的所有者
     * @param projectId 目标项目的Id
     * @param projectCode 目标项目的代码
     * @param authority 需要的权限（例如 readable、writable、deletable）
     * @return 是否有权限
     */
    boolean hasProjectAuthority(String targetUserName, Integer projectId, String projectCode, String authority);

    /**
     * 根据分组Id检查当前用户是否有对应权限。
     *
     * @param groupId 分组Id
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasGroupAuthority(Integer groupId, String authority);

    /**
     * 根据分组对象检查当前用户是否有对应权限。
     *
     * @param group 分组对象
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasGroupAuthority(MockGroup group, String authority);

    /**
     * 根据请求Id检查当前用户是否有对应权限。
     *
     * @param requestId 请求Id
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasRequestAuthority(Integer requestId, String authority);

    /**
     * 根据请求对象检查当前用户是否有对应权限。
     *
     * @param request 请求对象
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasRequestAuthority(MockRequest request, String authority);

    /**
     * 根据响应数据Id检查当前用户是否有对应权限。
     *
     * @param dataId 响应数据Id
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasDataAuthority(Integer dataId, String authority);

    /**
     * 根据响应数据对象检查当前用户是否有对应权限。
     *
     * @param data 响应数据对象
     * @param authority 需要的权限
     * @return 是否有权限
     */
    boolean hasDataAuthority(MockData data, String authority);
}
