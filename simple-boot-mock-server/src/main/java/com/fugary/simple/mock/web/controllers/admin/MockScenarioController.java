package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockScenarioQueryVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;
import static com.fugary.simple.mock.utils.security.SecurityUtils.getLoginUser;

@RestController
@RequestMapping("/admin/scenarios")
public class MockScenarioController {

    @Autowired
    private MockScenarioService mockScenarioService;

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockRequestService mockRequestService;

    @GetMapping
    public SimpleResult<List<MockScenario>> search(@ModelAttribute MockScenarioQueryVo queryVo) {
        if (queryVo.getGroupId() == null) {
            return SimpleResultUtils.createSimpleResult(List.of());
        }
        MockGroup group = mockGroupService.getById(queryVo.getGroupId());
        if (group == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockScenarioService.list(Wrappers.<MockScenario>query()
                .eq("group_id", queryVo.getGroupId())
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus())
                .orderByAsc("id")));
    }

    @PostMapping
    public SimpleResult<MockScenario> save(@RequestBody MockScenario scenario) {
        if (scenario.getGroupId() == null || StringUtils.isBlank(scenario.getScenarioName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400);
        }
        MockGroup group = mockGroupService.getById(scenario.getGroupId());
        if (group == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        if (scenario.getId() == null) {
            scenario.setScenarioCode(SimpleMockUtils.uuid());
        }
        if (scenario.getStatus() == null) {
            scenario.setStatus(1);
        }
        if (mockScenarioService.existsMockScenario(scenario)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        if (StringUtils.isBlank(scenario.getCreator()) && getLoginUser() != null) {
            scenario.setCreator(getLoginUser().getUserName());
        }
        mockScenarioService.saveOrUpdate(SimpleMockUtils.addAuditInfo(scenario));
        if (scenario.getCopyFromScenarioCode() != null) {
            mockScenarioService.copyScenarioRequests(group.getId(), scenario.getCopyFromScenarioCode(),
                    scenario.getScenarioCode());
        }
        if (!scenario.isEnabled() && StringUtils.equals(group.getActiveScenarioCode(), scenario.getScenarioCode())) {
            group.setActiveScenarioCode(null);
            mockGroupService.updateById(SimpleMockUtils.addAuditInfo(group));
        }
        return SimpleResultUtils.createSimpleResult(scenario);
    }

    @PostMapping("/activate")
    public SimpleResult<MockGroup> activate(@RequestBody MockScenarioQueryVo queryVo) {
        if (queryVo.getGroupId() == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400);
        }
        MockGroup group = mockGroupService.getById(queryVo.getGroupId());
        if (group == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        String scenarioCode = StringUtils.trimToNull(queryVo.getScenarioCode());
        if (StringUtils.isNotBlank(scenarioCode)) {
            MockScenario scenario = mockScenarioService.getOne(Wrappers.<MockScenario>query()
                    .eq("group_id", group.getId())
                    .eq("scenario_code", scenarioCode)
                    .eq("status", 1));
            if (scenario == null) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
            }
        }
        group.setActiveScenarioCode(scenarioCode);
        mockGroupService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(group));
        return SimpleResultUtils.createSimpleResult(group);
    }

    @PostMapping("/toggleStatus/{id}")
    @Transactional
    public SimpleResult<MockScenario> toggleStatus(@PathVariable("id") Integer id) {
        MockScenario scenario = mockScenarioService.getById(id);
        if (scenario == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        MockGroup group = mockGroupService.getById(scenario.getGroupId());
        if (group == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        scenario.setStatus(scenario.isEnabled() ? 0 : 1);
        mockScenarioService.updateById(SimpleMockUtils.addAuditInfo(scenario));
        if (!scenario.isEnabled() && StringUtils.equals(group.getActiveScenarioCode(), scenario.getScenarioCode())) {
            group.setActiveScenarioCode(null);
            mockGroupService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(group));
        }
        return SimpleResultUtils.createSimpleResult(scenario);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public SimpleResult<Object> remove(@PathVariable("id") Integer id) {
        MockScenario scenario = mockScenarioService.getById(id);
        if (scenario == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        MockGroup group = mockGroupService.getById(scenario.getGroupId());
        if (group == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        List<Integer> requestIds = mockRequestService.list(Wrappers.<MockRequest>query()
                .eq("group_id", scenario.getGroupId())
                .eq("scenario_code", scenario.getScenarioCode())
                .isNull(DB_MODIFY_FROM_KEY))
                .stream().map(MockRequest::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(requestIds)) {
            mockRequestService.deleteMockRequests(requestIds);
        }
        mockScenarioService.removeById(id);
        if (StringUtils.equals(group.getActiveScenarioCode(), scenario.getScenarioCode())) {
            group.setActiveScenarioCode(null);
            mockGroupService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(group));
        }
        return SimpleResultUtils.createSimpleResult(true);
    }
}
