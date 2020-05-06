package com.mengstudy.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengstudy.simple.mock.entity.mock.MockUser;
import com.mengstudy.simple.mock.mapper.mock.MockUserMapper;
import com.mengstudy.simple.mock.service.mock.MockUserService;
import org.springframework.stereotype.Service;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockUserServiceImpl extends ServiceImpl<MockUserMapper, MockUser> implements MockUserService {
}
