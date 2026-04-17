package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockLogService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;

import com.fugary.simple.mock.web.vo.dashboard.DashboardMetricsVo;
import com.fugary.simple.mock.web.vo.dashboard.DashboardTopApiVo;
import com.fugary.simple.mock.web.vo.NameValueObj;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fugary.simple.mock.utils.security.SecurityUtils;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    private static final String DASHBOARD_LOG_NAME = "MockController#doMock";
    private static final String LOG_COUNT_ALIAS = "log_count";
    private static final String LOG_DAY_ALIAS = "log_day";

    @Autowired
    private MockLogService mockLogService;

    @Autowired
    private MockProjectService mockProjectService;

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockDataService mockDataService;

    private Object getMapValue(Map<String, Object> map, String key) {
        if (map == null)
            return null;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private QueryWrapper<MockLog> buildDashboardLogQuery(String userName) {
        return Wrappers.<MockLog>query()
                .eq("log_name", DASHBOARD_LOG_NAME)
                .eq(StringUtils.isNotBlank(userName), "user_name", userName);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String valueText = StringUtils.trimToNull(String.valueOf(value));
        if (valueText == null) {
            return null;
        }
        try {
            return Integer.valueOf(valueText);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String valueText = StringUtils.trimToNull(String.valueOf(value));
        if (valueText == null) {
            return 0L;
        }
        try {
            return Long.parseLong(valueText);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        return toInteger(getMapValue(map, key));
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Integer value = getIntegerValue(map, key);
        return value != null ? value : 0;
    }

    private long getLongValue(Map<String, Object> map, String key) {
        return toLong(getMapValue(map, key));
    }

    private String getDateKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return DateFormatUtils.format((Date) value, "yyyy-MM-dd");
        }
        String valueText = StringUtils.trimToNull(String.valueOf(value));
        if (valueText == null) {
            return null;
        }
        return valueText.length() > 10 ? valueText.substring(0, 10) : valueText;
    }

    private Set<Integer> collectIntegerValues(List<Map<String, Object>> maps, String key) {
        Set<Integer> values = new LinkedHashSet<>();
        for (Map<String, Object> map : maps) {
            Integer value = getIntegerValue(map, key);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private Map<Integer, MockData> loadActiveMockDataMap(Collection<Integer> dataIds) {
        if (dataIds == null || dataIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mockDataService.listByIds(dataIds).stream()
                .filter(Objects::nonNull)
                .filter(mockData -> mockData.getId() != null
                        && mockData.getModifyFrom() == null
                        && mockData.isEnabled()
                        && mockData.getRequestId() != null)
                .collect(Collectors.toMap(MockData::getId, mockData -> mockData, (first, second) -> first));
    }

    private Map<Integer, MockRequest> loadActiveMockRequestMap(Collection<Integer> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mockRequestService.listByIds(requestIds).stream()
                .filter(Objects::nonNull)
                .filter(mockRequest -> mockRequest.getId() != null
                        && mockRequest.getModifyFrom() == null
                        && mockRequest.isEnabled()
                        && mockRequest.getGroupId() != null)
                .collect(Collectors.toMap(MockRequest::getId, mockRequest -> mockRequest, (first, second) -> first));
    }

    private Map<Integer, MockGroup> loadActiveMockGroupMap(Collection<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return mockGroupService.listByIds(groupIds).stream()
                .filter(Objects::nonNull)
                .filter(mockGroup -> mockGroup.getId() != null
                        && mockGroup.getModifyFrom() == null
                        && mockGroup.isEnabled())
                .collect(Collectors.toMap(MockGroup::getId, mockGroup -> mockGroup, (first, second) -> first));
    }

    private DashboardRelationContext loadDashboardRelationContext(Collection<Integer> dataIds) {
        Map<Integer, MockData> dataMap = loadActiveMockDataMap(dataIds);
        Map<Integer, MockRequest> requestMap = loadActiveMockRequestMap(dataMap.values().stream()
                .map(MockData::getRequestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Integer, MockGroup> groupMap = loadActiveMockGroupMap(requestMap.values().stream()
                .map(MockRequest::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        return new DashboardRelationContext(dataMap, requestMap, groupMap);
    }

    private String buildProjectKey(String userName, String projectCode) {
        String normalizedProjectCode = StringUtils.trimToNull(projectCode);
        if (normalizedProjectCode == null) {
            return null;
        }
        String normalizedUserName = StringUtils.equalsIgnoreCase(MockConstants.MOCK_DEFAULT_PROJECT, normalizedProjectCode)
                ? StringUtils.EMPTY : StringUtils.trimToEmpty(userName);
        return normalizedUserName + "_" + normalizedProjectCode;
    }

    private Map<Integer, String> loadGroupProjectNameMap(Collection<MockGroup> mockGroups) {
        if (mockGroups == null || mockGroups.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Integer> projectIds = mockGroups.stream()
                .map(MockGroup::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, MockProject> projectIdMap = projectIds.isEmpty() ? Collections.emptyMap()
                : mockProjectService.listByIds(projectIds).stream()
                .filter(Objects::nonNull)
                .filter(project -> project.getId() != null)
                .collect(Collectors.toMap(MockProject::getId, project -> project, (first, second) -> first));
        Set<String> projectCodes = mockGroups.stream()
                .filter(mockGroup -> mockGroup.getProjectId() == null)
                .map(MockGroup::getProjectCode)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, MockProject> projectCodeMap = projectCodes.isEmpty() ? Collections.emptyMap()
                : mockProjectService.list(Wrappers.<MockProject>query().in("project_code", projectCodes)).stream()
                .filter(Objects::nonNull)
                .filter(project -> StringUtils.isNotBlank(project.getProjectCode()))
                .collect(Collectors.toMap(project -> buildProjectKey(project.getUserName(), project.getProjectCode()),
                        project -> project, (first, second) -> first));
        Map<Integer, String> groupProjectNameMap = new HashMap<>();
        for (MockGroup mockGroup : mockGroups) {
            if (mockGroup.getId() == null) {
                continue;
            }
            String projectName = null;
            if (mockGroup.getProjectId() != null) {
                MockProject project = projectIdMap.get(mockGroup.getProjectId());
                projectName = project != null ? project.getProjectName() : null;
            }
            if (StringUtils.isBlank(projectName)) {
                MockProject project = projectCodeMap.get(buildProjectKey(mockGroup.getUserName(), mockGroup.getProjectCode()));
                projectName = project != null ? project.getProjectName() : StringUtils.trimToNull(mockGroup.getProjectCode());
            }
            if (StringUtils.isNotBlank(projectName)) {
                groupProjectNameMap.put(mockGroup.getId(), projectName);
            }
        }
        return groupProjectNameMap;
    }

    private Map<String, MockGroup> loadGroupPathMap(Collection<String> groupPaths) {
        if (groupPaths == null || groupPaths.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> normalizedGroupPaths = groupPaths.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedGroupPaths.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, MockGroup> groupPathMap = new HashMap<>();
        List<MockGroup> mockGroups = mockGroupService.list(Wrappers.<MockGroup>query()
                .in("group_path", normalizedGroupPaths)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY));
        for (MockGroup mockGroup : mockGroups) {
            String groupPath = StringUtils.trimToNull(mockGroup.getGroupPath());
            if (groupPath != null) {
                groupPathMap.putIfAbsent(groupPath, mockGroup);
            }
        }
        return groupPathMap;
    }

    @GetMapping("/metrics")
    public SimpleResult<DashboardMetricsVo> metrics(@RequestParam(defaultValue = "false") boolean all) {
        DashboardMetricsVo metrics = new DashboardMetricsVo();
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        Map<String, Object> todayMetrics = mockLogService.getMap(buildDashboardLogQuery(userName)
                .select("COUNT(*) as today_total",
                        "SUM(CASE WHEN data_id IS NULL AND proxy_url IS NULL THEN 1 ELSE 0 END) as today_error")
                .ge("create_date", today));
        long todayTotal = getLongValue(todayMetrics, "today_total");
        long todayError = getLongValue(todayMetrics, "today_error");
        long totalCalls = mockLogService.count(buildDashboardLogQuery(userName));
        long totalMockGroups = mockGroupService.count(Wrappers.<MockGroup>query()
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1));
        long totalMockApis = mockRequestService.count(Wrappers.<MockRequest>query()
                .eq(StringUtils.isNotBlank(userName), "creator", userName)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1));
        long totalMockData = mockDataService.count(Wrappers.<MockData>query()
                .eq(StringUtils.isNotBlank(userName), "creator", userName)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1));

        metrics.setTodayTotal(todayTotal);
        metrics.setTodayError(todayError);
        metrics.setTotalCalls(totalCalls);
        metrics.setTotalMockGroups(totalMockGroups);
        metrics.setTotalMockApis(totalMockApis);
        metrics.setTotalMockData(totalMockData);

        return SimpleResultUtils.createSimpleResult(metrics);
    }

    @GetMapping("/trend")
    public SimpleResult<List<NameValueObj>> trend(@RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean all) {
        days = Math.max(days, 1);
        List<NameValueObj> trends = new ArrayList<>();
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        Date startDate = DateUtils.addDays(today, -days + 1);
        String userName = all ? null : SecurityUtils.getLoginUserName();
        List<Map<String, Object>> trendMaps = mockLogService.listMaps(buildDashboardLogQuery(userName)
                .select("CAST(create_date AS DATE) as " + LOG_DAY_ALIAS, "COUNT(*) as " + LOG_COUNT_ALIAS)
                .ge("create_date", startDate)
                .groupBy("CAST(create_date AS DATE)"));
        Map<String, Integer> trendCountMap = new HashMap<>();
        for (Map<String, Object> map : trendMaps) {
            String logDay = getDateKey(getMapValue(map, LOG_DAY_ALIAS));
            if (logDay != null) {
                trendCountMap.put(logDay, getIntValue(map, LOG_COUNT_ALIAS));
            }
        }
        for (int i = days - 1; i >= 0; i--) {
            Date currentDate = DateUtils.addDays(today, -i);
            NameValueObj trendVo = new NameValueObj();
            trendVo.setName(DateFormatUtils.format(currentDate, "MM-dd"));
            trendVo.setValue(trendCountMap.getOrDefault(DateFormatUtils.format(currentDate, "yyyy-MM-dd"), 0));
            trends.add(trendVo);
        }
        return SimpleResultUtils.createSimpleResult(trends);
    }

    @GetMapping("/project-activity")
    public SimpleResult<List<NameValueObj>> projectActivity(@RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean all) {
        days = Math.max(days, 1);
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days + 1);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        QueryWrapper<MockLog> queryWrapper = buildDashboardLogQuery(userName)
                .select("data_id, COUNT(*) as log_count")
                .isNotNull("data_id")
                .ge("create_date", startDate)
                .groupBy("data_id");

        List<Map<String, Object>> maps = mockLogService.listMaps(queryWrapper);
        DashboardRelationContext relationContext = loadDashboardRelationContext(collectIntegerValues(maps, "data_id"));
        Map<Integer, String> groupProjectNameMap = loadGroupProjectNameMap(relationContext.getGroups());
        Map<String, Integer> projectActivityMap = new HashMap<>();
        for (Map<String, Object> map : maps) {
            Integer dataId = getIntegerValue(map, "data_id");
            if (dataId == null) {
                continue;
            }
            MockData mockData = relationContext.getData(dataId);
            MockRequest mockRequest = relationContext.getRequest(mockData);
            MockGroup mockGroup = relationContext.getGroup(mockRequest);
            if (mockGroup == null) {
                continue;
            }
            String projectName = groupProjectNameMap.get(mockGroup.getId());
            if (StringUtils.isBlank(projectName)) {
                continue;
            }
            projectActivityMap.merge(projectName, getIntValue(map, LOG_COUNT_ALIAS), Integer::sum);
        }

        List<NameValueObj> activities = projectActivityMap.entrySet().stream()
                .map(entry -> {
                    NameValueObj vo = new NameValueObj();
                    vo.setName(entry.getKey());
                    vo.setValue(entry.getValue());
                    return vo;
                })
                .sorted((a, b) -> ((Integer) b.getValue()).compareTo((Integer) a.getValue())) // descending
                .collect(Collectors.toList());

        return SimpleResultUtils.createSimpleResult(activities);
    }

    @GetMapping("/top-apis")
    public SimpleResult<List<DashboardTopApiVo>> topApis(@RequestParam(required = false) String logResult,
            @RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "false") boolean all) {
        limit = Math.max(limit, 1);
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -30); // limit to last 30 days
        String userName = all ? null : SecurityUtils.getLoginUserName();

        QueryWrapper<MockLog> mockQueryWrapper = buildDashboardLogQuery(userName)
                .select("data_id, request_url, COUNT(*) as log_count")
                .isNotNull("data_id")
                .eq(StringUtils.isNotBlank(logResult), "log_result", logResult)
                .ge("create_date", startDate)
                .groupBy("data_id", "request_url")
                .orderByDesc("log_count")
                .last("LIMIT " + (limit * 2)); // fetch more in case multiple data map to same request

        List<Map<String, Object>> mockMaps = mockLogService.listMaps(mockQueryWrapper);
        DashboardRelationContext relationContext = loadDashboardRelationContext(collectIntegerValues(mockMaps, "data_id"));
        Map<String, DashboardTopApiVo> requestMap = new HashMap<>();
        for (Map<String, Object> map : mockMaps) {
            Integer dataId = getIntegerValue(map, "data_id");
            if (dataId == null) {
                continue;
            }
            MockData mockData = relationContext.getData(dataId);
            MockRequest mockRequest = relationContext.getRequest(mockData);
            if (mockRequest == null) {
                continue;
            }
            MockGroup mockGroup = relationContext.getGroup(mockRequest);
            String requestUrl = (String) getMapValue(map, "request_url");
            String key = mockRequest.getId() + "_" + StringUtils.defaultString(requestUrl);
            DashboardTopApiVo vo = requestMap.get(key);
            if (vo == null) {
                vo = new DashboardTopApiVo();
                vo.setName(mockRequest.getRequestName());
                vo.setValue(0);
                if (mockGroup != null) {
                    vo.setGroup(mockGroup);
                }
                vo.setPath(StringUtils.isNotBlank(requestUrl) ? requestUrl
                        : StringUtils.defaultString(mockGroup != null ? mockGroup.getGroupPath() : "")
                        + StringUtils.defaultString(mockRequest.getRequestPath()));
                vo.setProxy(false);
                requestMap.put(key, vo);
            }
            vo.setValue(vo.getValue() + getIntValue(map, LOG_COUNT_ALIAS));
        }

        List<DashboardTopApiVo> topApis = new ArrayList<>(requestMap.values());

        // 2. Proxy requests
        QueryWrapper<MockLog> proxyQueryWrapper = buildDashboardLogQuery(userName)
                .select("request_url, mock_group_path, COUNT(*) as log_count")
                .isNull("data_id")
                .isNotNull("proxy_url")
                .eq(StringUtils.isNotBlank(logResult), "log_result", logResult)
                .ge("create_date", startDate)
                .groupBy("request_url", "mock_group_path")
                .orderByDesc("log_count")
                .last("LIMIT " + limit);
        List<Map<String, Object>> proxyMaps = mockLogService.listMaps(proxyQueryWrapper);
        Map<String, MockGroup> proxyGroupMap = loadGroupPathMap(proxyMaps.stream()
                .map(map -> (String) getMapValue(map, "mock_group_path"))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        for (Map<String, Object> map : proxyMaps) {
            String requestUrl = (String) getMapValue(map, "request_url");
            String mockGroupPath = (String) getMapValue(map, "mock_group_path");
            DashboardTopApiVo vo = new DashboardTopApiVo();
            vo.setName("代理透传服务");
            MockGroup mockGroup = StringUtils.isNotBlank(mockGroupPath)
                    ? proxyGroupMap.get(StringUtils.trimToNull(mockGroupPath)) : null;
            if (mockGroup != null || StringUtils.isNotBlank(mockGroupPath)) {
                if (mockGroup == null) {
                    mockGroup = new MockGroup();
                }
                vo.setGroup(mockGroup);
            } else {
                MockGroup unknownGroup = new MockGroup();
                unknownGroup.setGroupName(null); // Leave null so it triggers default i18n in front-end
                vo.setGroup(unknownGroup);
            }
            vo.setPath(requestUrl);
            vo.setValue(getIntValue(map, LOG_COUNT_ALIAS));
            vo.setProxy(true);
            topApis.add(vo);
        }

        topApis = topApis.stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        return SimpleResultUtils.createSimpleResult(topApis);
    }

    @GetMapping("/mock-vs-proxy")
    public SimpleResult<List<NameValueObj>> mockVsProxy(@RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean all) {
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days + 1);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        long mockCount = mockLogService.count(Wrappers.<MockLog>query()
                .isNotNull("data_id")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate));

        long proxyCount = mockLogService.count(Wrappers.<MockLog>query()
                .isNull("data_id")
                .isNotNull("proxy_url")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate));

        long noReturnCount = mockLogService.count(Wrappers.<MockLog>query()
                .isNull("data_id")
                .isNull("proxy_url")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate));

        List<NameValueObj> results = new ArrayList<>();
        NameValueObj mockVo = new NameValueObj();
        mockVo.setName("Mock返回");
        mockVo.setValue((int) mockCount);
        results.add(mockVo);

        NameValueObj proxyVo = new NameValueObj();
        proxyVo.setName("代理返回");
        proxyVo.setValue((int) proxyCount);
        results.add(proxyVo);

        NameValueObj noReturnVo = new NameValueObj();
        noReturnVo.setName("无返回");
        noReturnVo.setValue((int) noReturnCount);
        results.add(noReturnVo);

        return SimpleResultUtils.createSimpleResult(results);
    }

    @GetMapping("/top-user-calls")
    public SimpleResult<List<NameValueObj>> topUserCalls(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "false") boolean all) {
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        QueryWrapper<MockLog> queryWrapper = Wrappers.<MockLog>query()
                .select("user_name, COUNT(*) as log_count")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .isNotNull("user_name")
                .ne("user_name", "")
                .ge("create_date", startDate)
                .groupBy("user_name")
                .orderByDesc("log_count")
                .last("LIMIT " + limit);

        List<Map<String, Object>> maps = mockLogService.listMaps(queryWrapper);

        List<NameValueObj> results = maps.stream().map(map -> {
            NameValueObj vo = new NameValueObj();
            vo.setName((String) getMapValue(map, "user_name"));
            Number numValue = (Number) getMapValue(map, "log_count");
            vo.setValue(numValue != null ? numValue.intValue() : 0);
            return vo;
        }).collect(Collectors.toList());

        return SimpleResultUtils.createSimpleResult(results);
    }

    @GetMapping("/top-user-groups")
    public SimpleResult<List<NameValueObj>> topUserGroups(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean all) {
        String userName = all ? null : SecurityUtils.getLoginUserName();

        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .select("user_name, COUNT(*) as log_count")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .isNotNull("user_name")
                .ne("user_name", "")
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1)
                .groupBy("user_name")
                .orderByDesc("log_count")
                .last("LIMIT " + limit);

        List<Map<String, Object>> maps = mockGroupService.listMaps(queryWrapper);

        List<NameValueObj> results = maps.stream().map(map -> {
            NameValueObj vo = new NameValueObj();
            vo.setName((String) getMapValue(map, "user_name"));
            Number numValue = (Number) getMapValue(map, "log_count");
            vo.setValue(numValue != null ? numValue.intValue() : 0);
            return vo;
        }).collect(Collectors.toList());

        return SimpleResultUtils.createSimpleResult(results);
    }

    @GetMapping("/public-vs-private")
    public SimpleResult<List<NameValueObj>> publicVsPrivate(@RequestParam(defaultValue = "false") boolean all) {
        String userName = all ? null : SecurityUtils.getLoginUserName();

        long publicProjects = mockProjectService.count(Wrappers.<MockProject>query()
                .eq("public_flag", true)
                .eq("status", 1));

        long privateProjects = mockProjectService.count(Wrappers.<MockProject>query()
                .and(StringUtils.isNotBlank(userName),
                        w -> w.eq("user_name", userName).or().isNull("user_name").or().eq("user_name", ""))
                .and(w -> w.eq("public_flag", false).or().isNull("public_flag"))
                .eq("status", 1));

        List<NameValueObj> results = new ArrayList<>();
        NameValueObj pubVo = new NameValueObj();
        pubVo.setName("公开项目");
        pubVo.setValue((int) publicProjects);
        results.add(pubVo);

        NameValueObj privVo = new NameValueObj();
        privVo.setName("私有项目");
        privVo.setValue((int) privateProjects);
        results.add(privVo);

        return SimpleResultUtils.createSimpleResult(results);
    }

    private static class DashboardRelationContext {

        private final Map<Integer, MockData> dataMap;
        private final Map<Integer, MockRequest> requestMap;
        private final Map<Integer, MockGroup> groupMap;

        private DashboardRelationContext(Map<Integer, MockData> dataMap, Map<Integer, MockRequest> requestMap,
                Map<Integer, MockGroup> groupMap) {
            this.dataMap = dataMap;
            this.requestMap = requestMap;
            this.groupMap = groupMap;
        }

        private MockData getData(Integer dataId) {
            return dataMap.get(dataId);
        }

        private MockRequest getRequest(MockData mockData) {
            return mockData == null ? null : requestMap.get(mockData.getRequestId());
        }

        private MockGroup getGroup(MockRequest mockRequest) {
            return mockRequest == null ? null : groupMap.get(mockRequest.getGroupId());
        }

        private Collection<MockGroup> getGroups() {
            return groupMap.values();
        }
    }
}
