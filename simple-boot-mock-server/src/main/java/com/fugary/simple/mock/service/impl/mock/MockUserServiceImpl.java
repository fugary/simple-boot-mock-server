package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.mapper.mock.MockUserMapper;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockUserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockUserServiceImpl extends ServiceImpl<MockUserMapper, MockUser> implements MockUserService {

    @Autowired
    private MockGroupService mockGroupService;

    @Override
    public boolean deleteMockUser(Integer id) {
        getOptById(id).ifPresent(mockUser -> {
            mockGroupService.list(Wrappers.<MockGroup>query().eq("user_name", mockUser.getUserName()))
                    .forEach(mockGroup -> {
                        mockGroupService.deleteMockGroup(mockGroup.getId());
                    });
        });
        return true;
    }

    @Override
    public boolean existsUser(MockUser user) {
        List<MockUser> exists = list(Wrappers.<MockUser>query().eq("user_name", user.getUserName()));
        return exists.stream().anyMatch(existGroup -> !existGroup.getId().equals(user.getId()));
    }

    @Override
    public String encryptPassword(String password) {
        return DigestUtils.sha256Hex(password);
    }

    @Override
    public boolean matchPassword(String password, String encryptPassword) {
        return StringUtils.equalsIgnoreCase(encryptPassword(password), encryptPassword);
    }
}
