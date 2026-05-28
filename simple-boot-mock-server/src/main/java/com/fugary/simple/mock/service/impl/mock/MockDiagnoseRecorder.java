package com.fugary.simple.mock.service.impl.mock;

import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockScenario;
import com.fugary.simple.mock.web.vo.result.MockDiagnoseVo;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class MockDiagnoseRecorder {

    private static final String STATUS_INFO = "info";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_DANGER = "danger";

    private static final String STAGE_INIT = "init";
    private static final String STAGE_GROUP = "group";
    private static final String STAGE_SCENARIO = "scenario";
    private static final String STAGE_REQUEST_CANDIDATES = "request_candidates";
    private static final String STAGE_REQUEST_FORCE = "request_force";
    private static final String STAGE_REQUEST_PATH = "request_path";
    private static final String STAGE_REQUEST_PATTERN = "request_pattern";
    private static final String STAGE_REQUEST = "request";
    private static final String STAGE_DATA_FORCE = "data_force";
    private static final String STAGE_DATA_CANDIDATES = "data_candidates";
    private static final String STAGE_DATA_PATTERN = "data_pattern";
    private static final String STAGE_DATA_DEFAULT = "data_default";

    private static final String CODE_REQUEST_RECEIVED = "request_received";
    private static final String CODE_GROUP_NOT_FOUND = "group_not_found";
    private static final String CODE_GROUP_MATCHED = "group_matched";
    private static final String CODE_GROUP_CHECK_FAILED = "group_check_failed";
    private static final String CODE_GROUP_DISABLED = "group_disabled";
    private static final String CODE_GROUP_PAUSED = "group_paused";
    private static final String CODE_GROUP_PATH_EMPTY = "group_path_empty";
    private static final String CODE_SCENARIO_NOT_FOUND = "scenario_not_found";
    private static final String CODE_SCENARIO_SELECTED = "scenario_selected";
    private static final String CODE_SCENARIO_MATCHED = "scenario_matched";
    private static final String CODE_REQUEST_CANDIDATES_LOADED = "request_candidates_loaded";
    private static final String CODE_FORCE_REQUEST_NOT_FOUND = "force_request_not_found";
    private static final String CODE_FORCE_REQUEST_SELECTED = "force_request_selected";
    private static final String CODE_REQUEST_PATH_MATCHED = "request_path_matched";
    private static final String CODE_REQUEST_PATTERN_MATCHED = "request_pattern_matched";
    private static final String CODE_REQUEST_PATTERN_NOT_MATCHED = "request_pattern_not_matched";
    private static final String CODE_REQUEST_PATTERN_ERROR = "request_pattern_error";
    private static final String CODE_REQUEST_DISABLED = "request_disabled";
    private static final String CODE_REQUEST_PAUSED = "request_paused";
    private static final String CODE_REQUEST_PATH_NOT_MATCHED = "request_path_not_matched";
    private static final String CODE_REQUEST_PATTERN_NOT_MATCHED_ALL = "request_pattern_not_matched_all";
    private static final String CODE_FORCE_DATA_SELECTED = "force_data_selected";
    private static final String CODE_FORCE_DATA_NOT_FOUND = "force_data_not_found_in_request";
    private static final String CODE_DATA_CANDIDATES_LOADED = "data_candidates_loaded";
    private static final String CODE_DATA_PATTERN_NOT_MATCHED = "data_pattern_not_matched";
    private static final String CODE_DATA_PATTERN_MATCHED = "data_pattern_matched";
    private static final String CODE_DEFAULT_DATA_NOT_FOUND = "default_data_not_found";
    private static final String CODE_DEFAULT_DATA_SELECTED = "default_data_selected";

    private static final String KEY_ACTIVE_SCENARIO_CODE = "activeScenarioCode";
    private static final String KEY_CANDIDATES = "candidates";
    private static final String KEY_CONTENT_TYPE = "contentType";
    private static final String KEY_COUNT = "count";
    private static final String KEY_DATA = "data";
    private static final String KEY_DATA_ID = "dataId";
    private static final String KEY_DATA_NAME = "dataName";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_ENABLED_COUNT = "enabledCount";
    private static final String KEY_FORCE_REQUEST = "forceRequest";
    private static final String KEY_GROUP = "group";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_GROUP_NAME = "groupName";
    private static final String KEY_GROUP_PATH = "groupPath";
    private static final String KEY_MATCH_PATTERN = "matchPattern";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_METHOD = "method";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_PATH = "path";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_REQUEST_ID = "requestId";
    private static final String KEY_REQUEST_NAME = "requestName";
    private static final String KEY_REQUEST_PATH = "requestPath";
    private static final String KEY_SCENARIO = "scenario";
    private static final String KEY_SCENARIO_CODE = "scenarioCode";
    private static final String KEY_SCENARIO_ID = "scenarioId";
    private static final String KEY_SCENARIO_NAME = "scenarioName";
    private static final String KEY_STATUS_CODE = "statusCode";
    private static final String KEY_TOTAL = "total";

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
        if (!isEnabled()) {
            return;
        }
        step(STAGE_INIT, STATUS_INFO, CODE_REQUEST_RECEIVED,
                KEY_PATH, requestPath, KEY_METHOD, method, KEY_GROUP_PATH, requestGroupPath);
    }

    void groupMatched(MockGroup group, String groupPath) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setGroupInfo(group);
        step(STAGE_GROUP, group == null ? STATUS_DANGER : STATUS_SUCCESS,
                group == null ? CODE_GROUP_NOT_FOUND : CODE_GROUP_MATCHED,
                KEY_GROUP_PATH, groupPath, KEY_GROUP, groupInfo(group));
    }

    void groupCheckFailed(MockGroup group) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_GROUP, STATUS_DANGER, CODE_GROUP_CHECK_FAILED, KEY_GROUP, groupInfo(group));
    }

    void groupDisabled(MockGroup group) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setGroupInfo(group);
        step(STAGE_GROUP, STATUS_WARNING, CODE_GROUP_DISABLED, KEY_GROUP, groupInfo(group));
    }

    void groupPaused(MockGroup group) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setGroupInfo(group);
        step(STAGE_GROUP, STATUS_WARNING, CODE_GROUP_PAUSED, KEY_GROUP, groupInfo(group));
    }

    void groupPathEmpty(String requestPath) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_GROUP, STATUS_DANGER, CODE_GROUP_PATH_EMPTY, KEY_PATH, requestPath);
    }

    void scenarioSelected(String activeScenarioCode, Supplier<MockScenario> scenarioSupplier) {
        if (!isEnabled() || StringUtils.isBlank(activeScenarioCode)) {
            return;
        }
        MockScenario scenario = scenarioSupplier.get();
        diagnose.setScenarioInfo(activeScenarioCode, scenario);
        step(STAGE_SCENARIO, scenario == null ? STATUS_WARNING : STATUS_SUCCESS,
                scenario == null ? CODE_SCENARIO_NOT_FOUND : CODE_SCENARIO_SELECTED,
                KEY_SCENARIO, scenarioInfo(activeScenarioCode, scenario));
    }

    void scenarioMatched(String scenarioCode, Supplier<MockScenario> scenarioSupplier) {
        if (!isEnabled() || StringUtils.isBlank(scenarioCode)) {
            return;
        }
        MockScenario scenario = scenarioSupplier.get();
        diagnose.setScenarioInfo(scenarioCode, scenario);
        step(STAGE_SCENARIO, scenario == null ? STATUS_WARNING : STATUS_SUCCESS,
                scenario == null ? CODE_SCENARIO_NOT_FOUND : CODE_SCENARIO_MATCHED,
                KEY_SCENARIO, scenarioInfo(scenarioCode, scenario));
    }

    void requestCandidates(List<MockRequest> requests) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_REQUEST_CANDIDATES, requests.isEmpty() ? STATUS_WARNING : STATUS_SUCCESS,
                CODE_REQUEST_CANDIDATES_LOADED,
                KEY_COUNT, requests.size(), KEY_CANDIDATES, requestInfos(requests));
    }

    void forceRequestSelected(Integer requestId, List<MockRequest> requests) {
        if (!isEnabled() || requestId == null || requestId == 0) {
            return;
        }
        MockRequest request = requests.stream().findFirst().orElse(null);
        if (request != null) {
            diagnose.setRequestInfo(request);
        }
        step(STAGE_REQUEST_FORCE, request == null ? STATUS_WARNING : STATUS_SUCCESS,
                request == null ? CODE_FORCE_REQUEST_NOT_FOUND : CODE_FORCE_REQUEST_SELECTED,
                KEY_REQUEST_ID, requestId, KEY_REQUEST, requestInfo(request));
    }

    void requestPathMatched(MockRequest request) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setRequestInfo(request);
        step(STAGE_REQUEST_PATH, STATUS_SUCCESS, CODE_REQUEST_PATH_MATCHED, KEY_REQUEST, requestInfo(request));
    }

    void requestPatternMatched(MockRequest request, boolean matched, boolean testRequest) {
        if (!isEnabled()) {
            return;
        }
        if (StringUtils.isNotBlank(request.getMatchPattern())) {
            step(STAGE_REQUEST_PATTERN, matched || testRequest ? STATUS_SUCCESS : STATUS_WARNING,
                    matched ? CODE_REQUEST_PATTERN_MATCHED : CODE_REQUEST_PATTERN_NOT_MATCHED,
                    KEY_REQUEST, requestInfo(request), KEY_FORCE_REQUEST, testRequest);
        }
    }

    void requestPatternError(MockRequest request, RuntimeException e) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_REQUEST_PATTERN, STATUS_DANGER, CODE_REQUEST_PATTERN_ERROR,
                KEY_REQUEST, requestInfo(request), KEY_MESSAGE, e.getMessage());
    }

    void requestDisabled(MockRequest request) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setRequestInfo(request);
        step(STAGE_REQUEST, STATUS_WARNING, CODE_REQUEST_DISABLED, KEY_REQUEST, requestInfo(request));
    }

    void requestPaused(MockRequest request) {
        if (!isEnabled()) {
            return;
        }
        diagnose.setRequestInfo(request);
        step(STAGE_REQUEST, STATUS_WARNING, CODE_REQUEST_PAUSED, KEY_REQUEST, requestInfo(request));
    }

    void requestNotMatched(int pathMatchedCount, int requestCount) {
        if (!isEnabled()) {
            return;
        }
        if (pathMatchedCount == 0) {
            step(STAGE_REQUEST_PATH, STATUS_DANGER, CODE_REQUEST_PATH_NOT_MATCHED, KEY_COUNT, requestCount);
        } else if (pathMatchedCount > 1) {
            step(STAGE_REQUEST_PATTERN, STATUS_DANGER, CODE_REQUEST_PATTERN_NOT_MATCHED_ALL,
                    KEY_COUNT, pathMatchedCount);
        }
    }

    void forceDataSelected(MockData data) {
        if (!isEnabled()) {
            return;
        }
        setDataInfo(data);
        if (data != null) {
            step(STAGE_DATA_FORCE, STATUS_SUCCESS, CODE_FORCE_DATA_SELECTED, KEY_DATA, dataInfo(data));
        }
    }

    void forceDataNotFound(Integer dataId, MockRequest request) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_DATA_FORCE, STATUS_WARNING, CODE_FORCE_DATA_NOT_FOUND,
                KEY_DATA_ID, dataId, KEY_REQUEST, requestInfo(request));
    }

    void dataCandidates(List<MockData> allData, int enabledCount) {
        if (!isEnabled()) {
            return;
        }
        step(STAGE_DATA_CANDIDATES, enabledCount == 0 ? STATUS_WARNING : STATUS_SUCCESS,
                CODE_DATA_CANDIDATES_LOADED,
                KEY_TOTAL, allData.size(),
                KEY_ENABLED_COUNT, enabledCount,
                KEY_CANDIDATES, dataInfos(allData));
    }

    void dataPatternMatched(MockData data, List<MockData> dataList) {
        if (!isEnabled()) {
            return;
        }
        setDataInfo(data);
        if (dataList.stream().anyMatch(item -> StringUtils.isNotBlank(item.getMatchPattern()))) {
            step(STAGE_DATA_PATTERN, data == null ? STATUS_WARNING : STATUS_SUCCESS,
                    data == null ? CODE_DATA_PATTERN_NOT_MATCHED : CODE_DATA_PATTERN_MATCHED,
                    KEY_DATA, dataInfo(data));
        }
    }

    void defaultDataSelected(MockData data) {
        if (!isEnabled()) {
            return;
        }
        setDataInfo(data);
        step(STAGE_DATA_DEFAULT, data == null ? STATUS_WARNING : STATUS_SUCCESS,
                data == null ? CODE_DEFAULT_DATA_NOT_FOUND : CODE_DEFAULT_DATA_SELECTED,
                KEY_DATA, dataInfo(data));
    }

    private void setDataInfo(MockData data) {
        diagnose.setDataInfo(data);
    }

    private void step(String stage, String status, String code, Object... details) {
        if (isEnabled()) {
            diagnose.step(stage, status, code, details);
        }
    }

    private Map<String, Object> groupInfo(MockGroup group) {
        if (group == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        put(info, KEY_GROUP_ID, group.getId());
        put(info, KEY_GROUP_NAME, group.getGroupName());
        put(info, KEY_GROUP_PATH, group.getGroupPath());
        put(info, KEY_ENABLED, group.isEnabled());
        put(info, KEY_PAUSED, Boolean.TRUE.equals(group.getDisableMock()));
        put(info, KEY_ACTIVE_SCENARIO_CODE, group.getActiveScenarioCode());
        return info;
    }

    private Map<String, Object> scenarioInfo(String scenarioCode, MockScenario scenario) {
        Map<String, Object> info = new LinkedHashMap<>();
        put(info, KEY_SCENARIO_ID, scenario == null ? null : scenario.getId());
        put(info, KEY_SCENARIO_NAME, scenario == null ? null : scenario.getScenarioName());
        put(info, KEY_SCENARIO_CODE, scenario == null ? scenarioCode : scenario.getScenarioCode());
        return info;
    }

    private List<Map<String, Object>> requestInfos(List<MockRequest> requests) {
        return requests.stream().map(this::requestInfo).collect(Collectors.toList());
    }

    private Map<String, Object> requestInfo(MockRequest request) {
        if (request == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        put(info, KEY_REQUEST_ID, request.getId());
        put(info, KEY_REQUEST_NAME, request.getRequestName());
        put(info, KEY_ENABLED, request.isEnabled());
        put(info, KEY_PAUSED, Boolean.TRUE.equals(request.getDisableMock()));
        put(info, KEY_METHOD, request.getMethod());
        put(info, KEY_REQUEST_PATH, request.getRequestPath());
        put(info, KEY_SCENARIO_CODE, request.getScenarioCode());
        put(info, KEY_MATCH_PATTERN, request.getMatchPattern());
        return info;
    }

    private List<Map<String, Object>> dataInfos(List<MockData> dataList) {
        return dataList.stream().map(this::dataInfo).collect(Collectors.toList());
    }

    private Map<String, Object> dataInfo(MockData data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> info = new LinkedHashMap<>();
        put(info, KEY_DATA_ID, data.getId());
        put(info, KEY_DATA_NAME, data.getDataName());
        put(info, KEY_ENABLED, data.isEnabled());
        put(info, KEY_STATUS_CODE, data.getStatusCode());
        put(info, KEY_CONTENT_TYPE, data.getContentType());
        put(info, KEY_MATCH_PATTERN, data.getMatchPattern());
        return info;
    }

    private void put(Map<String, Object> info, String key, Object value) {
        if (value != null && (!(value instanceof String) || StringUtils.isNotBlank((String) value))) {
            info.put(key, value);
        }
    }
}
