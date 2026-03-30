package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockProjectUser;

import java.util.List;

/**
 * 协作成员Service
 * Create date 2026/03/30
 *
 * @author gary.fu
 */
public interface MockProjectUserService extends IService<MockProjectUser> {

    /**
     * 加载项目的所有协作成员
     *
     * @param projectCode
     * @return
     */
    List<MockProjectUser> loadProjectUsers(String projectCode);

}
