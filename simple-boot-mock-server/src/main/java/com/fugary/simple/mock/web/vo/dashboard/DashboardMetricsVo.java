package com.fugary.simple.mock.web.vo.dashboard;

import lombok.Data;

@Data
public class DashboardMetricsVo {
    private Long todayTotal;
    private Long totalCalls;
    private Long totalMockGroups;
    private Long totalMockApis;
}
