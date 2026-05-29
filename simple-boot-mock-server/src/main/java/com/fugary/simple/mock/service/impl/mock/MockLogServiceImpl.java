package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.mapper.mock.MockLogMapper;
import com.fugary.simple.mock.service.mock.MockLogService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MockLogServiceImpl extends ServiceImpl<MockLogMapper, MockLog> implements MockLogService {

    @Override
    public int clearLogsBefore(Date expiredDate) {
        return getBaseMapper().delete(Wrappers.<MockLog>query().lt("create_date", expiredDate));
    }
}
