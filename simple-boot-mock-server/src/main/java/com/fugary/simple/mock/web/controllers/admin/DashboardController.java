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

    @GetMapping("/metrics")
    public SimpleResult<DashboardMetricsVo> metrics(@RequestParam(defaultValue = "false") boolean all) {
        DashboardMetricsVo metrics = new DashboardMetricsVo();
        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        long todayTotal = mockLogService.count(Wrappers.<MockLog>query()
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", today));
        long todayError = mockLogService.count(Wrappers.<MockLog>query()
                .eq("log_name", "MockController#doMock")
                .isNull("data_id")
                .isNull("proxy_url")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", today));
        long totalCalls = mockLogService.count(Wrappers.<MockLog>query()
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName));
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
        List<NameValueObj> trends = new ArrayList<>();
        Date now = new Date();
        Date today = DateUtils.truncate(now, Calendar.DATE);
        String userName = all ? null : SecurityUtils.getLoginUserName();
        for (int i = days - 1; i >= 0; i--) {
            Date startDate = DateUtils.addDays(today, -i);
            Date endDate = DateUtils.addDays(startDate, 1);
            long count = mockLogService.count(Wrappers.<MockLog>query()
                    .eq("log_name", "MockController#doMock")
                    .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                    .ge("create_date", startDate)
                    .lt("create_date", endDate));

            NameValueObj trendVo = new NameValueObj();
            trendVo.setName(DateFormatUtils.format(startDate, "MM-dd"));
            trendVo.setValue((int) count);
            trends.add(trendVo);
        }
        return SimpleResultUtils.createSimpleResult(trends);
    }

    @GetMapping("/project-activity")
    public SimpleResult<List<NameValueObj>> projectActivity(@RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean all) {
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -days + 1);
        String userName = all ? null : SecurityUtils.getLoginUserName();

        // Group by data_id and map to actual Project
        QueryWrapper<MockLog> queryWrapper = Wrappers.<MockLog>query()
                .select("data_id, COUNT(*) as log_count")
                .isNotNull("data_id")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate)
                .groupBy("data_id");

        List<Map<String, Object>> maps = mockLogService.listMaps(queryWrapper);

        Map<String, Integer> projectActivityMap = new HashMap<>();
        for (Map<String, Object> map : maps) {
            Object dataIdObj = getMapValue(map, "data_id");
            String dataId = dataIdObj != null ? String.valueOf(dataIdObj) : null;
            Number numValue = (Number) getMapValue(map, "log_count");
            Integer value = numValue != null ? numValue.intValue() : 0;
            if (dataId != null) {
                MockData mockData = mockDataService.getById(dataId);
                if (mockData != null && mockData.getModifyFrom() == null && mockData.isEnabled()
                        && mockData.getRequestId() != null) {
                    MockRequest mockRequest = mockRequestService.getById(mockData.getRequestId());
                    if (mockRequest != null && mockRequest.getModifyFrom() == null && mockRequest.isEnabled()
                            && mockRequest.getGroupId() != null) {
                        MockGroup mockGroup = mockGroupService.getById(mockRequest.getGroupId());
                        if (mockGroup != null && mockGroup.getModifyFrom() == null && mockGroup.isEnabled()
                                && StringUtils.isNotBlank(mockGroup.getProjectCode())) {
                            String projectCode = mockGroup.getProjectCode();
                            MockProject mockProject = mockProjectService
                                    .getOne(Wrappers.<MockProject>query().eq("project_code", projectCode));
                            String name = mockProject != null ? mockProject.getProjectName() : projectCode;
                            projectActivityMap.merge(name, value, (a, b) -> a + b);
                        }
                    }
                }
            }
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
        Date startDate = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -30); // limit to last 30 days
        String userName = all ? null : SecurityUtils.getLoginUserName();

        // 1. Mock Requests
        QueryWrapper<MockLog> mockQueryWrapper = Wrappers.<MockLog>query()
                .select("data_id, request_url, COUNT(*) as log_count")
                .isNotNull("data_id")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(logResult), "log_result", logResult)
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate)
                .groupBy("data_id", "request_url")
                .orderByDesc("log_count")
                .last("LIMIT " + (limit * 2)); // fetch more in case multiple data map to same request

        List<Map<String, Object>> mockMaps = mockLogService.listMaps(mockQueryWrapper);

        // Aggregate by request ID and requestUrl
        Map<String, DashboardTopApiVo> requestMap = new HashMap<>();
        for (Map<String, Object> map : mockMaps) {
            Object dataIdObj = getMapValue(map, "data_id");
            String dataId = dataIdObj != null ? String.valueOf(dataIdObj) : null;
            String requestUrl = (String) getMapValue(map, "request_url");
            Number numValue = (Number) getMapValue(map, "log_count");
            Integer value = numValue != null ? numValue.intValue() : 0;
            if (dataId != null) {
                MockData mockData = mockDataService.getById(dataId);
                if (mockData != null && mockData.getModifyFrom() == null && mockData.isEnabled()
                        && mockData.getRequestId() != null) {

                    MockRequest mockRequest = mockRequestService.getById(mockData.getRequestId());
                    if (mockRequest != null && mockRequest.getModifyFrom() == null && mockRequest.isEnabled()) {
                        String key = mockRequest.getId() + "_" + requestUrl;
                        DashboardTopApiVo vo = requestMap.get(key);
                        if (vo == null) {
                            vo = new DashboardTopApiVo();
                            vo.setName(mockRequest.getRequestName());
                            vo.setValue(0);
                            MockGroup mockGroup = mockGroupService.getById(mockRequest.getGroupId());
                            if (mockGroup != null) {
                                vo.setGroup(mockGroup);
                            }
                            vo.setPath(StringUtils.isNotBlank(requestUrl) ? requestUrl
                                    : StringUtils.defaultString(mockGroup != null ? mockGroup.getGroupPath() : "")
                                            + StringUtils.defaultString(mockRequest.getRequestPath()));
                            vo.setProxy(false);
                            requestMap.put(key, vo);
                        }
                        vo.setValue(vo.getValue() + value);
                    }
                }
            }
        }

        List<DashboardTopApiVo> topApis = new ArrayList<>(requestMap.values());

        // 2. Proxy requests
        QueryWrapper<MockLog> proxyQueryWrapper = Wrappers.<MockLog>query()
                .select("request_url, mock_group_path, COUNT(*) as log_count")
                .isNull("data_id")
                .isNotNull("proxy_url")
                .eq("log_name", "MockController#doMock")
                .eq(StringUtils.isNotBlank(logResult), "log_result", logResult)
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate)
                .groupBy("request_url", "mock_group_path")
                .orderByDesc("log_count")
                .last("LIMIT " + limit);
        List<Map<String, Object>> proxyMaps = mockLogService.listMaps(proxyQueryWrapper);
        for (Map<String, Object> map : proxyMaps) {
            String requestUrl = (String) getMapValue(map, "request_url");
            String mockGroupPath = (String) getMapValue(map, "mock_group_path");
            Number numValue = (Number) getMapValue(map, "log_count");
            Integer value = numValue != null ? numValue.intValue() : 0;

            DashboardTopApiVo vo = new DashboardTopApiVo();
            vo.setName("代理透传服务");
            MockGroup mockGroup = null;
            if (StringUtils.isNotBlank(mockGroupPath)) {
                List<MockGroup> groups = mockGroupService.list(Wrappers.<MockGroup>query()
                        .eq("group_path", mockGroupPath).isNull(MockConstants.DB_MODIFY_FROM_KEY));
                if (!groups.isEmpty()) {
                    mockGroup = groups.get(0);
                }
            }
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
            vo.setValue(value);
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
}
