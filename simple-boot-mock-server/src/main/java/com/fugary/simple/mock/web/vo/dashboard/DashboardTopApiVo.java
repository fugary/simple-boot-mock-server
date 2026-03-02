package com.fugary.simple.mock.web.vo.dashboard;

import com.fugary.simple.mock.entity.mock.MockGroup;
import lombok.Data;

@Data
public class DashboardTopApiVo {
    private String name;
    private String path;
    private MockGroup group;
    private Integer value;
    private boolean proxy;
}
