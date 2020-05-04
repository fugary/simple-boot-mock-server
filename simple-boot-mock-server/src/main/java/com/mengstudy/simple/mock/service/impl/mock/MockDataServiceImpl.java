package com.mengstudy.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.mapper.mock.MockDataMapper;
import com.mengstudy.simple.mock.service.mock.MockDataService;
import org.springframework.stereotype.Service;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockDataServiceImpl extends ServiceImpl<MockDataMapper, MockData> implements MockDataService {
}
