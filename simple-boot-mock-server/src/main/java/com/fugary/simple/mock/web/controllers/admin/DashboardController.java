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
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", today));
        long totalCalls = mockLogService.count(Wrappers.<MockLog>query()
                .eq(StringUtils.isNotBlank(userName), "user_name", userName));
        long totalMockGroups = mockGroupService.count(Wrappers.<MockGroup>query()
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1));
        long totalMockApis = mockRequestService.count(Wrappers.<MockRequest>query()
                .eq(StringUtils.isNotBlank(userName), "creator", userName)
                .isNull(MockConstants.DB_MODIFY_FROM_KEY)
                .eq("status", 1));

        metrics.setTodayTotal(todayTotal);
        metrics.setTotalCalls(totalCalls);
        metrics.setTotalMockGroups(totalMockGroups);
        metrics.setTotalMockApis(totalMockApis);

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

        QueryWrapper<MockLog> queryWrapper = Wrappers.<MockLog>query()
                .select("data_id, COUNT(*) as log_count")
                .isNotNull("data_id")
                .eq(StringUtils.isNotBlank(logResult), "log_result", logResult)
                .eq(StringUtils.isNotBlank(userName), "user_name", userName)
                .ge("create_date", startDate)
                .groupBy("data_id")
                .orderByDesc("log_count")
                .last("LIMIT " + (limit * 2)); // fetch more in case multiple data map to same request

        List<Map<String, Object>> maps = mockLogService.listMaps(queryWrapper);

        // Aggregate by request ID
        Map<Integer, Integer> requestCounts = new HashMap<>(); // requestId -> count
        for (Map<String, Object> map : maps) {
            Object dataIdObj = getMapValue(map, "data_id");
            String dataId = dataIdObj != null ? String.valueOf(dataIdObj) : null;
            Number numValue = (Number) getMapValue(map, "log_count");
            Integer value = numValue != null ? numValue.intValue() : 0;
            if (dataId != null) {
                MockData mockData = mockDataService.getById(dataId);
                if (mockData != null && mockData.getModifyFrom() == null && mockData.isEnabled()
                        && mockData.getRequestId() != null) {
                    requestCounts.merge(mockData.getRequestId(), value, (a, b) -> a + b);
                }
            }
        }

        List<DashboardTopApiVo> topApis = requestCounts.entrySet().stream()
                .map(entry -> {
                    MockRequest mockRequest = mockRequestService.getById(entry.getKey());
                    if (mockRequest != null && mockRequest.getModifyFrom() == null && mockRequest.isEnabled()) {
                        DashboardTopApiVo vo = new DashboardTopApiVo();
                        vo.setName(mockRequest.getRequestName());
                        vo.setPath(mockRequest.getRequestPath());
                        vo.setValue(entry.getValue());
                        return vo;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());

        return SimpleResultUtils.createSimpleResult(topApis);
    }
}
