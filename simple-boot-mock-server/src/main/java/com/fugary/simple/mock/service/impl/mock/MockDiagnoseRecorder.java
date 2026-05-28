package com.fugary.simple.mock.service.impl.mock;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Supplier;

class MockDiagnoseRecorder {

    private static final MockDiagnoseRecorder NOOP = new MockDiagnoseRecorder(null);

    private final MockDiagnoseVo diagnose;

    private MockDiagnoseRecorder(MockDiagnoseVo diagnose) {
        this.diagnose = diagnose;
    }

    static MockDiagnoseRecorder of(MockDiagnoseVo diagnose) {
        return diagnose == null ? NOOP : new MockDiagnoseRecorder(diagnose);
    }

    boolean isEnabled() {
        return diagnose != null;
    }

    void requestReceived(String requestPath, String method, String requestGroupPath) {
        step("init", "info", "request_received", "path", requestPath, "method", method,
                "groupPath", requestGroupPath);
    }

    void groupMatched(MockGroup group, String groupPath) {
        if (isEnabled()) {
            diagnose.setGroupInfo(group);
        }
        step("group", group == null ? "danger" : "success",
                group == null ? "group_not_found" : "group_matched", "groupPath", groupPath);
    }

    void groupCheckFailed(MockGroup group) {
        step("group", "danger", "group_check_failed", "groupId", group.getId());
    }

    void groupDisabled(MockGroup group) {
        step("group", "warning", "group_disabled", "groupId", group.getId());
    }

    void groupPathEmpty(String requestPath) {
        step("group", "danger", "group_path_empty", "path", requestPath);
    }

    void scenarioSkippedByTarget(String activeScenarioCode) {
        if (StringUtils.isNotBlank(activeScenarioCode)) {
            step("scenario", "info", "scenario_skipped_by_target", "scenarioCode", activeScenarioCode);
        }
    }

    void scenarioSelected(String activeScenarioCode, Supplier<MockScenario> scenarioSupplier) {
        if (StringUtils.isNotBlank(activeScenarioCode) && isEnabled()) {
            MockScenario scenario = scenarioSupplier.get();
            diagnose.setScenarioInfo(activeScenarioCode, scenario);
            step("scenario", scenario == null ? "warning" : "success",
                    scenario == null ? "scenario_not_found" : "scenario_selected",
                    "scenarioCode", activeScenarioCode,
                    "scenarioName", scenario == null ? null : scenario.getScenarioName());
        }
    }

    void requestCandidates(int count) {
        step("request_candidates", count == 0 ? "warning" : "success",
                "request_candidates_loaded", "count", count);
    }

    void forceRequestSelected(Integer requestId, List<MockRequest> requests) {
        if (requestId == null || requestId == 0) {
            return;
        }
        MockRequest request = requests.stream().findFirst().orElse(null);
        if (isEnabled() && request != null) {
            diagnose.setRequestInfo(request);
        }
        step("request_force", request == null ? "warning" : "success",
                request == null ? "force_request_not_found" : "force_request_selected",
                "requestId", requestId,
                "requestName", request == null ? null : request.getRequestName(),
                "path", request == null ? null : request.getRequestPath());
    }

    void requestPathMatched(MockRequest request) {
        if (isEnabled()) {
            diagnose.setRequestInfo(request);
        }
        step("request_path", "success", "request_path_matched",
                "requestId", request.getId(), "path", request.getRequestPath());
    }

    void requestPatternMatched(MockRequest request, boolean matched, boolean testRequest) {
        if (StringUtils.isNotBlank(request.getMatchPattern())) {
            step("request_pattern", matched || testRequest ? "success" : "warning",
                    matched ? "request_pattern_matched" : "request_pattern_not_matched",
                    "requestId", request.getId(), "forceRequest", testRequest);
        }
    }

    void requestPatternError(MockRequest request, RuntimeException e) {
        step("request_pattern", "danger", "request_pattern_error",
                "requestId", request.getId(), "message", e.getMessage());
    }

    void requestDisabled(MockRequest request) {
        step("request", "warning", "request_disabled", "requestId", request.getId());
    }

    void requestNotMatched(int pathMatchedCount, int requestCount) {
        if (pathMatchedCount == 0) {
            step("request_path", "danger", "request_path_not_matched", "count", requestCount);
        } else if (pathMatchedCount > 1) {
            step("request_pattern", "danger", "request_pattern_not_matched_all", "count", pathMatchedCount);
        }
    }

    void forceDataSelected(MockData data) {
        setDataInfo(data);
        if (data != null) {
            step("data_force", "success", "force_data_selected", "dataId", data.getId());
        }
    }

    void forceDataNotFound(Integer dataId, MockRequest request) {
        step("data_force", "warning", "force_data_not_found_in_request",
                "dataId", dataId, "requestId", request.getId());
    }

    void dataCandidates(int totalCount, int enabledCount) {
        step("data_candidates", enabledCount == 0 ? "warning" : "success",
                "data_candidates_loaded", "total", totalCount, "enabled", enabledCount);
    }

    void dataPatternMatched(MockData data, List<MockData> dataList) {
        setDataInfo(data);
        if (isEnabled() && dataList.stream().anyMatch(item -> StringUtils.isNotBlank(item.getMatchPattern()))) {
            step("data_pattern", data == null ? "warning" : "success",
                    data == null ? "data_pattern_not_matched" : "data_pattern_matched",
                    "dataId", data == null ? null : data.getId());
        }
    }

    void defaultDataSelected(MockData data) {
        setDataInfo(data);
        step("data_default", data == null ? "warning" : "success",
                data == null ? "default_data_not_found" : "default_data_selected",
                "dataId", data == null ? null : data.getId());
    }

    private void setDataInfo(MockData data) {
        if (isEnabled()) {
            diagnose.setDataInfo(data);
        }
    }

    private void step(String stage, String status, String code, Object... details) {
        if (isEnabled()) {
            diagnose.step(stage, status, code, details);
        }
    }
}
