package com.fugary.simple.mock.service.mock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fugary.simple.mock.entity.mock.MockScenario;

public interface MockScenarioService extends IService<MockScenario> {

    boolean existsMockScenario(MockScenario scenario);

    /**
     * Copy requests, data, and schemas from one scenario to another
     * 
     * @param groupId
     * @param fromScenarioCode
     * @param toScenarioCode
     */
    void copyScenarioRequests(Integer groupId, String fromScenarioCode, String toScenarioCode);
}
