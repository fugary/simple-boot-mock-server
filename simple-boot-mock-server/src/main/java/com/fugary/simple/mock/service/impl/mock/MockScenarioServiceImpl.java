package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.mapper.mock.MockScenarioMapper;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockScenarioServiceImpl extends ServiceImpl<MockScenarioMapper, MockScenario>
        implements MockScenarioService {

    @Override
    public boolean existsMockScenario(MockScenario scenario) {
        List<MockScenario> exists = list(Wrappers.<MockScenario>query()
                .eq("group_id", scenario.getGroupId())
                .eq("scenario_name", scenario.getScenarioName()));
        return exists.stream().anyMatch(item -> !item.getId().equals(scenario.getId()));
    }
}
