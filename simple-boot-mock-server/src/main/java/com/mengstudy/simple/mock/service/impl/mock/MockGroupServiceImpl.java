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
import com.mengstudy.simple.mock.utils.SimpleMockUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Service
public class MockGroupServiceImpl extends ServiceImpl<MockGroupMapper, MockGroup> implements MockGroupService, InitializingBean {

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockDataService mockDataService;

    @Setter
    @Getter
    private String mockPrefix = MockConstants.MOCK_PREFIX;

    @Getter
    @Setter
    private PathMatcher pathMatcher = new AntPathMatcher();

    @Getter
    @Setter
    private Pattern groupPathPattern = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        groupPathPattern = Pattern.compile(getMockPrefix() + "/(\\w+).*");
    }

    /**
     * 正则表达式计算成group_path的值
     *
     * @param requestPath
     * @return
     */
    public String calcGroupPath(String requestPath) {
        Matcher matcher = groupPathPattern.matcher(requestPath);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

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
    public MockData matchMockData(String requestPath, String method, Integer defaultId) {
        String requestGroupPath = calcGroupPath(requestPath);
        if (StringUtils.isNotBlank(requestGroupPath)) {
            MockGroup mockGroup = getOne(Wrappers.<MockGroup>query()
                    .eq("group_path", requestGroupPath)
                    .eq("status", 1));
            if (mockGroup != null) {
                // 查询Request
                List<MockRequest> mockRequests = mockRequestService.list(Wrappers.<MockRequest>query()
                        .eq("group_id", mockGroup.getId())
                        .eq("status", 1));
                String groupPath = getMockPrefix() + StringUtils.prependIfMissing(mockGroup.getGroupPath(), "/");
                // 请求是否匹配上Request，如果匹配上就查询Data
                for (MockRequest mockRequest : mockRequests) {
                    if (pathMatcher.match(groupPath + StringUtils.prependIfMissing(mockRequest.getRequestPath(), "/"), requestPath)
                            && StringUtils.equalsIgnoreCase(method, mockRequest.getMethod())) {
                        List<MockData> mockDataList = mockDataService.list(Wrappers.<MockData>query()
                                .eq("request_id", mockRequest.getId())
                                .eq("status", 1));
                        return findMockData(mockDataList, defaultId);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查询MockData
     *
     * @param mockDataList
     * @param defaultId
     * @return
     */
    private MockData findMockData(List<MockData> mockDataList, Integer defaultId) {
        MockData result = null;
        for (MockData mockData : mockDataList) {
            if (defaultId != null && defaultId.equals(mockData.getId())) { // 强制指定
                return mockData;
            }
            if (result == null || (!SimpleMockUtils.isDefault(result) && SimpleMockUtils.isDefault(mockData))) {
                result = mockData;
            }
        }
        return result;
    }

}
