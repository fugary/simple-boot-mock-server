package com.fugary.simple.mock.web.vo.result;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockScenario;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class MockDiagnoseVo {

    private String resultType = "none";

    private Item group;

    private Item scenario;

    private Item request;

    private Item data;

    private String proxyUrl;

    private List<Step> steps = new ArrayList<>();

    public void finish(String resultType, String code, Object... details) {
        this.resultType = resultType;
        step("result", calcStatus(resultType), code, details);
    }

    public void step(String stage, String status, String code, Object... details) {
        Step step = new Step();
        step.setStage(stage);
        step.setStatus(status);
        step.setCode(code);
        step.setDetails(toDetails(details));
        steps.add(step);
    }

    public void setGroupInfo(MockGroup group) {
        this.group = group == null ? null : new Item(group.getId(), group.getGroupPath(), group.getGroupName());
    }

    public void setScenarioInfo(String scenarioCode, MockScenario scenario) {
        this.scenario = new Item(scenario == null ? null : scenario.getId(),
                scenario == null ? scenarioCode : scenario.getScenarioCode(),
                scenario == null ? null : scenario.getScenarioName());
    }

    public void setRequestInfo(MockRequest request) {
        this.request = request == null ? null : new Item(request.getId(), request.getRequestPath(), request.getRequestName());
    }

    public void setDataInfo(MockData data) {
        this.data = data == null ? null : new Item(data.getId(), String.valueOf(data.getStatusCode()), data.getDataName());
    }

    private Map<String, Object> toDetails(Object... details) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < details.length; i += 2) {
            map.put(String.valueOf(details[i]), details[i + 1]);
        }
        return map;
    }

    private String calcStatus(String resultType) {
        if ("mock".equals(resultType) || "proxy".equals(resultType)) {
            return "success";
        }
        return "error".equals(resultType) ? "danger" : "warning";
    }

    @Data
    public static class Item {

        private final Integer id;

        private final String key;

        private final String name;
    }

    @Data
    public static class Step {

        private String stage;

        private String status;

        private String code;

        private Map<String, Object> details;
    }
}
