package com.fugary.simple.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class SimpleBootMockServerApplicationTests {

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockProjectService mockProjectService;

    @Test
    void contextLoads() {
    }

    @Test
    void testTopFlagSortQuery() {
        // 验证 MockGroup 查询在真实 H2 数据库中执行 TOP_FLAG_SORT 不抛出语法异常
        List<MockGroup> groups = mockGroupService.list(Wrappers.<MockGroup>query()
                .orderByAsc(MockConstants.TOP_FLAG_SORT)
                .orderByDesc("id"));
        Assertions.assertNotNull(groups);

        // 验证 MockProject 查询在真实 H2 数据库中执行 TOP_FLAG_SORT 不抛出语法异常
        List<MockProject> projects = mockProjectService.list(Wrappers.<MockProject>query()
                .orderByAsc(MockConstants.TOP_FLAG_SORT)
                .orderByDesc("id"));
        Assertions.assertNotNull(projects);
    }
}
