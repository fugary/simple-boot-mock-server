package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.mapper.mock.MockProjectUserMapper;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
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
    public List<MockProjectUser> loadProjectUsers(String projectCode) {
        return this.list(Wrappers.<MockProjectUser>query().eq("project_code", projectCode));
    }

}
