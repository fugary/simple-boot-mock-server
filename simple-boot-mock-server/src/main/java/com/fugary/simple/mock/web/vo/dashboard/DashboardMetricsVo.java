package com.fugary.simple.mock.web.vo.dashboard;

import lombok.Data;

@Data
public class DashboardMetricsVo {
    private Long todayTotal;
    private Long todayError;
    private Long totalProjects;
    private Long totalMockApis;
}
