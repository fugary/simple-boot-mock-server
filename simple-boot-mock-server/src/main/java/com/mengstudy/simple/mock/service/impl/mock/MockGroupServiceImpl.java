package com.mengstudy.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.entity.mock.MockGroup;
import com.mengstudy.simple.mock.entity.mock.MockRequest;
import com.mengstudy.simple.mock.mapper.mock.MockGroupMapper;
import com.mengstudy.simple.mock.service.mock.MockDataService;
import com.mengstudy.simple.mock.service.mock.MockGroupService;
import com.mengstudy.simple.mock.service.mock.MockRequestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockGroupServiceImpl extends ServiceImpl<MockGroupMapper, MockGroup> implements MockGroupService {

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockDataService mockDataService;

    private PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean deleteMockGroup(Integer id) {
        mockRequestService.remove(Wrappers.<MockRequest>query().eq("group_id", id));
        mockDataService.remove(Wrappers.<MockData>query().eq("group_id", id));
        return this.removeById(id);
    }

    @Override
    public boolean existsMockGroup(MockGroup group) {
        List<MockGroup> existGroups = list(Wrappers.<MockGroup>query()
                .eq("group_path", group.getGroupPath()));
        return existGroups.stream().anyMatch(existGroup -> !existGroup.getId().equals(group.getId()));
    }

    @Override
    public MockData matchMockData(String requestPath, String method) {
        List<MockGroup> mockGroups = list(Wrappers.<MockGroup>query()
                .eq("status", 1));
        for (MockGroup mockGroup : mockGroups) {
            String groupPath = MockConstants.MOCK_PREFIX + mockGroup.getGroupPath();
            // 请求是否匹配上group，如果匹配上就查询Request
            if (pathMatcher.match(groupPath + MockConstants.ALL_PATH_PATTERN, requestPath)) {
                List<MockRequest> mockRequests = mockRequestService.list(Wrappers.<MockRequest>query()
                        .eq("group_id", mockGroup.getId())
                        .eq("status", 1));
                // 请求是否匹配上Request，如果匹配上就查询Data
                for (MockRequest mockRequest : mockRequests) {
                    if (pathMatcher.match(groupPath + mockRequest.getRequestPath(), requestPath)
                            && StringUtils.equalsIgnoreCase(method, mockRequest.getMethod())) {
                        List<MockData> mockDataList = mockDataService.list(Wrappers.<MockData>query()
                                .eq("request_id", mockRequest.getId())
                                .eq("status", 1));
                        return mockDataList.stream().findFirst().orElse(null);
                    }
                }
            }
        }
        return null;
    }
}
