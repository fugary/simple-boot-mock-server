package com.fugary.simple.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
class SimpleBootMockServerApplicationTests {

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockProjectService mockProjectService;

    @Autowired
    private MockRequestService mockRequestService;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void shouldPersistAndClearRoundRobinKeyExpressionWithHistory() {
        MockRequest request = new MockRequest();
        request.setMethod("POST");
        request.setRequestPath("/round-robin-test");
        request.setLoadBalancer(MockConstants.MOCK_REQUEST_LOAD_BALANCE_ROUND_ROBIN);
        request.setRoundRobinKeyExpression("request.body.id");
        Assertions.assertTrue(mockRequestService.saveOrUpdate(request));

        MockRequest saved = mockRequestService.getById(request.getId());
        Assertions.assertEquals("request.body.id", saved.getRoundRobinKeyExpression());
        saved = SimpleMockUtils.copy(saved, MockRequest.class);
        saved.setRoundRobinKeyExpression("");
        Assertions.assertTrue(mockRequestService.saveOrUpdate(saved));
        Assertions.assertEquals("", mockRequestService.getById(request.getId()).getRoundRobinKeyExpression());
        MockRequest history = mockRequestService.getOne(Wrappers.<MockRequest>query()
                .eq("modify_from", request.getId()));
        Assertions.assertEquals("request.body.id", history.getRoundRobinKeyExpression());
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
