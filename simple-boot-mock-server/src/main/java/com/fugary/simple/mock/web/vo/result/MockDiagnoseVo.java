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

    private static final String KEY_STATUS_CODE = "statusCode";
    private static final String KEY_CONTENT_TYPE = "contentType";
    private static final String KEY_DURATION_MS = "durationMs";

    private String resultType = "none";

    private Item group;

    private Item scenario;

    private Item request;

    private Item data;

    private String proxyUrl;

    private String contentType;

    private Integer statusCode;

    private Long durationMs;

    private List<Step> steps = new ArrayList<>();

    public synchronized void finish(String resultType, String code, Object... details) {
        this.resultType = resultType;
        step("result", calcStatus(resultType), code, details);
        appendResultDetails(steps.get(steps.size() - 1));
    }

    public synchronized void completeHttpInfo(Integer statusCode, String contentType, Long durationMs) {
        if (statusCode != null) {
            this.statusCode = statusCode;
        }
        if (contentType != null) {
            this.contentType = contentType;
        }
        if (durationMs != null) {
            this.durationMs = durationMs;
        }
        appendResultDetails();
    }

    public synchronized void step(String stage, String status, String code, Object... details) {
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
        setScenarioInfo(scenarioCode, scenario, false);
    }

    public void setScenarioInfo(String scenarioCode, MockScenario scenario, boolean defaultScenario) {
        this.scenario = new Item(scenario == null ? null : scenario.getId(),
                scenario == null ? scenarioCode : scenario.getScenarioCode(),
                scenario == null ? null : scenario.getScenarioName());
        this.scenario.setDefaultScenario(defaultScenario ? Boolean.TRUE : null);
    }

    public void setRequestInfo(MockRequest request) {
        this.request = request == null ? null : new Item(request.getId(), request.getRequestPath(), request.getRequestName());
    }

    public void setDataInfo(MockData data) {
        setDataInfo(data, data == null ? null : data.getContentType());
    }

    public void setDataInfo(MockData data, String contentType) {
        this.data = data == null ? null : new DataItem(data.getId(), data.getDataName(),
                data.getStatusCode(), contentType);
    }

    private Map<String, Object> toDetails(Object... details) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < details.length; i += 2) {
            map.put(String.valueOf(details[i]), details[i + 1]);
        }
        return map;
    }

    private void appendResultDetails(Step step) {
        if (statusCode != null) {
            step.getDetails().put(KEY_STATUS_CODE, statusCode);
        }
        if (contentType != null) {
            step.getDetails().put(KEY_CONTENT_TYPE, contentType);
        }
        if (durationMs != null) {
            step.getDetails().put(KEY_DURATION_MS, durationMs);
        }
    }

    private void appendResultDetails() {
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step step = steps.get(i);
            if ("result".equals(step.getStage())) {
                appendResultDetails(step);
                return;
            }
        }
    }

    private String calcStatus(String resultType) {
        if ("mock".equals(resultType) || "proxy".equals(resultType)) {
            return "success";
        }
        return "error".equals(resultType) || "none".equals(resultType) ? "danger" : "info";
    }

    @Data
    public static class Item {

        private final Integer id;

        private final String key;

        private final String name;

        private Boolean defaultScenario;
    }

    @Data
    public static class DataItem extends Item {

        private final Integer statusCode;

        private final String contentType;

        public DataItem(Integer id, String name, Integer statusCode, String contentType) {
            super(id, null, name);
            this.statusCode = statusCode;
            this.contentType = contentType;
        }
    }

    @Data
    public static class Step {

        private String stage;

        private String status;

        private String code;

        private Map<String, Object> details;
    }
}
