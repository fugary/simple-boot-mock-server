package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.mapper.mock.MockUserMapper;
import com.fugary.simple.mock.service.mock.MockUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockUserServiceImpl extends ServiceImpl<MockUserMapper, MockUser> implements MockUserService {

    @Override
    public boolean existsUser(MockUser user) {
        List<MockUser> exists = list(Wrappers.<MockUser>query().eq("user_name", user.getUserName()));
        return exists.stream().anyMatch(existGroup -> !existGroup.getId().equals(user.getId()));
    }
}
