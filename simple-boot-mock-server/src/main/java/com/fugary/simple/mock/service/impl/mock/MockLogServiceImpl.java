package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.mapper.mock.MockLogMapper;
import com.fugary.simple.mock.service.mock.MockLogService;
import org.springframework.stereotype.Service;

@Service
public class MockLogServiceImpl extends ServiceImpl<MockLogMapper, MockLog> implements MockLogService {
}
