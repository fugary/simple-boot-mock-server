package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockScenario;

public interface MockScenarioService extends IService<MockScenario> {

    boolean existsMockScenario(MockScenario scenario);
}
