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
import com.mengstudy.simple.mock.utils.MockJsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2020/5/3 22:37 .<br>
 *
 * @author gary.fu
 */
@Slf4j
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
    public Pair<MockGroup, MockData> matchMockData(HttpServletRequest request, Integer defaultId) {
        String requestPath = request.getServletPath();
        String method = request.getMethod();
        String requestGroupPath = calcGroupPath(requestPath);
        MockGroup mockGroup = null;
        if (StringUtils.isNotBlank(requestGroupPath)) {
            mockGroup = getOne(Wrappers.<MockGroup>query()
                    .eq("group_path", requestGroupPath)
                    .eq("status", 1));
            if (mockGroup != null) {
                // 查询Request
                List<MockRequest> mockRequests = mockRequestService.list(Wrappers.<MockRequest>query()
                        .eq("group_id", mockGroup.getId())
                        .eq("method", method)
                        .eq("status", 1));
                String groupPath = getMockPrefix() + StringUtils.prependIfMissing(mockGroup.getGroupPath(), "/");
                // 请求是否匹配上Request，如果匹配上就查询Data
                for (MockRequest mockRequest : sortMockRequests(mockRequests)) {
                    String configRequestPath = StringUtils.prependIfMissing(mockRequest.getRequestPath(), "/");
                    configRequestPath = configRequestPath.replaceAll(":([\\w-]+)", "{$1}"); // spring 支持的ant path不支持:var格式，只支持{var}格式
                    String configPath = groupPath + configRequestPath;
                    if (pathMatcher.match(configPath, requestPath) && matchRequestPattern(request, mockRequest)) {
                        MockData mockData = mockRequestService.findMockData(mockRequest, defaultId);
                        processMockData(request, mockData, configPath, requestPath);
                        return Pair.of(mockGroup, mockData);
                    }
                }
            }
        }
        return Pair.of(mockGroup, null);
    }

    protected List<MockRequest> sortMockRequests(List<MockRequest> mockRequests) {
        mockRequests.sort((o1, o2) -> {
            // 按照matchPattern属性排序，长度最长的排前面
            return StringUtils.length(o2.getMatchPattern()) - StringUtils.length(o1.getMatchPattern());
        });
        return mockRequests;
    }

    protected boolean matchRequestPattern(HttpServletRequest request, MockRequest mockRequest) {
        if (StringUtils.isNotBlank(mockRequest.getMatchPattern())) {
            Object result = MockJsUtils.eval("Boolean(" + mockRequest.getMatchPattern() + ")"); // 转Boolean值
            log.info("requestPath={}, 计算：{}={}", mockRequest.getRequestPath(), mockRequest.getMatchPattern(), result);
            return Boolean.TRUE.equals(result); // 必须等于true才执行
        }
        return true;
    }

    /**
     * 处理MockData数据
     *
     * @param request
     * @param mockData
     * @param configPath
     * @param requestPath
     */
    protected void processMockData(HttpServletRequest request, MockData mockData, String configPath, String requestPath) {
        if (mockData != null) {
            String responseBody = StringUtils.trimToEmpty(mockData.getResponseBody());
            Map<String, String> variables = pathMatcher.extractUriTemplateVariables(configPath, requestPath);
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                variables.put(paramName, StringUtils.trimToEmpty(request.getParameter(paramName)));
            }
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                responseBody = responseBody
                        .replace(StringUtils.join("{{", entry.getKey(), "}}"), entry.getValue())
                        .replace(StringUtils.join("${", entry.getKey(), "}"), entry.getValue());
            }
            mockData.setResponseBody(MockJsUtils.mock(responseBody)); // 使用Mockjs来处理响应数据
        }
    }

}
