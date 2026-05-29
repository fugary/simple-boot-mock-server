package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockLog;

import java.util.Date;

public interface MockLogService extends IService<MockLog> {

    int clearLogsBefore(Date expiredDate);
}
