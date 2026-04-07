package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.mapper.mock.MockProjectUserMapper;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 协作成员ServiceImpl
 * Create date 2026/03/30
 *
 * @author gary.fu
 */
@Service
public class MockProjectUserServiceImpl extends ServiceImpl<MockProjectUserMapper, MockProjectUser> implements MockProjectUserService {

    @Override
    public List<MockProjectUser> loadProjectUsers(Integer projectId) {
        return loadProjectUsers(projectId, null);
    }

    @Override
    public List<MockProjectUser> loadProjectUsers(Integer projectId, String projectCode) {
        return this.list(Wrappers.<MockProjectUser>query().and(wrapper -> {
            wrapper.eq(projectId != null, "project_id", projectId);
            if (projectId != null && StringUtils.isNotBlank(projectCode)) {
                wrapper.or(legacy -> legacy.isNull("project_id").eq("project_code", projectCode));
            } else if (StringUtils.isNotBlank(projectCode)) {
                wrapper.eq("project_code", projectCode);
            }
        }));
    }

}
