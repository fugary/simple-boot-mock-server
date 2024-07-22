package com.fugary.simple.mock.service.impl.mock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockBase;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.imports.MockGroupImporter;
import com.fugary.simple.mock.mapper.mock.MockGroupMapper;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.MockJsUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.export.ExportDataVo;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.export.ExportRequestVo;
import com.fugary.simple.mock.web.vo.http.HttpRequestVo;
import com.fugary.simple.mock.web.vo.query.MockGroupImportParamVo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

    @Autowired
    private List<MockGroupImporter> mockGroupImporters = new ArrayList<>();

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
    public Triple<MockGroup, MockRequest, MockData> matchMockData(HttpServletRequest request, Integer requestId, Integer defaultId) {
        String requestPath = request.getServletPath();
        String method = request.getMethod();
        String requestGroupPath = calcGroupPath(requestPath);
        boolean testRequest = requestId != 0;
        MockGroup mockGroup = null;
        if (StringUtils.isNotBlank(requestGroupPath)) {
            mockGroup = getOne(Wrappers.<MockGroup>query()
                    .eq("group_path", requestGroupPath)
                    .eq(!testRequest,"status", 1));
            if (mockGroup != null) {
                // 查询Request
                QueryWrapper<MockRequest> requestQuery = Wrappers.<MockRequest>query().eq("group_id", mockGroup.getId())
                        .eq("method", method)
                        .eq(!testRequest, "status", 1)
                        .eq(testRequest, "id", requestId);
                List<MockRequest> mockRequests = mockRequestService.list(requestQuery);
                String groupPath = getMockPrefix() + StringUtils.prependIfMissing(mockGroup.getGroupPath(), "/");
                // 请求是否匹配上Request，如果匹配上就查询Data
                for (MockRequest mockRequest : sortMockRequests(mockRequests)) {
                    String configRequestPath = StringUtils.prependIfMissing(mockRequest.getRequestPath(), "/");
                    configRequestPath = configRequestPath.replaceAll(":([\\w-]+)", "{$1}"); // spring 支持的ant path不支持:var格式，只支持{var}格式
                    String configPath = groupPath + configRequestPath;
                    if (pathMatcher.match(configPath, requestPath)) {
                        try {
                            HttpRequestVo requestVo = calcRequestVo(request, configPath, requestPath);
                            MockJsUtils.setCurrentRequestVo(requestVo);
                            if (matchRequestPattern(mockRequest.getMatchPattern()) || testRequest) {
                                List<MockData> mockDataList = mockRequestService.loadAllDataByRequest(mockRequest.getId());
                                MockData mockData = mockRequestService.findForceMockData(mockDataList, defaultId);
                                mockDataList = mockDataList.stream().filter(MockBase::isEnabled).collect(Collectors.toList());
                                if (mockData == null) { // request匹配的数据查找
                                    mockData = mockRequestService.findMockDataByRequest(mockDataList, requestVo);
                                }
                                if (mockData == null) { // 没有配置参数匹，或者没有匹配，过滤掉配置有参数匹配的数据
                                    mockData = mockRequestService.findMockData(mockDataList);
                                }
                                processMockData(mockData, requestVo);
                                return Triple.of(mockGroup, mockRequest, mockData);
                            }
                        } finally {
                            MockJsUtils.removeCurrentRequestVo();
                        }
                    }
                }
            }
        }
        return Triple.of(mockGroup, null, null);
    }

    @Override
    public Integer calcDelayTime(MockGroup group, MockRequest request, MockData mockData) {
        if (mockData != null && mockData.getDelay() != null) {
            return mockData.getDelay();
        }
        if (request != null && request.getDelay() != null) {
            return request.getDelay();
        }
        if (group != null && group.getDelay() != null) {
            return group.getDelay();
        }
        return null;
    }

    @Override
    public void delayTime(long stateTime, Integer delayTime) {
        if (delayTime != null) {
            long nowTime = System.currentTimeMillis();
            long sleepTime = stateTime + delayTime - nowTime;
            if (sleepTime > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    log.error("delayTime error", e);
                }
            }
        }
    }

    protected List<MockRequest> sortMockRequests(List<MockRequest> mockRequests) {
        mockRequests.sort((o1, o2) -> {
            // 按照matchPattern属性排序，长度最长的排前面
            return StringUtils.length(o2.getMatchPattern()) - StringUtils.length(o1.getMatchPattern());
        });
        return mockRequests;
    }

    protected boolean matchRequestPattern(String matchPattern) {
        if (StringUtils.isNotBlank(matchPattern)) {
            Object result = scriptEngineProvider.eval("Boolean(" + matchPattern + ")"); // 转Boolean值
            log.info("计算匹配：{}={}", matchPattern, result);
            return Boolean.TRUE.equals(result); // 必须等于true才执行
        }
        return true;
    }

    /**
     * 计算HttpRequestVo信息
     *
     * @param request
     * @param configPath
     * @param requestPath
     * @return
     */
    protected HttpRequestVo calcRequestVo(HttpServletRequest request, String configPath, String requestPath) {
        HttpRequestVo requestVo = HttpRequestUtils.parseRequestVo(request);
        if (pathMatcher.match(configPath, requestPath)) {
            Map<String, String> variables = pathMatcher.extractUriTemplateVariables(configPath, requestPath);
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                variables.put(paramName, StringUtils.trimToEmpty(request.getParameter(paramName)));
            }
            requestVo.setPathParameters(variables);
        }
        return requestVo;
    }

    /**
     * 处理MockData数据，生成需要的模拟数据
     *
     * @param mockData
     * @param requestVo
     */
    protected void processMockData(MockData mockData, HttpRequestVo requestVo) {
        if (mockData != null) {
            String responseBody = StringUtils.trimToEmpty(mockData.getResponseBody());
            responseBody = MockJsUtils.processResponseBody(responseBody, requestVo, paramKey -> scriptEngineProvider.eval(paramKey));
            mockData.setResponseBody(scriptEngineProvider.mock(responseBody)); // 使用Mockjs来处理响应数据
        }
    }

    @Override
    public List<ExportGroupVo> loadExportGroups(List<MockGroup> groups) {
        List<Integer> groupIds = groups.stream().map(MockGroup::getId).collect(Collectors.toList());
        List<MockRequest> mockRequests = mockRequestService.list(Wrappers.<MockRequest>query().in("group_id", groupIds));
        List<MockData> mockDataList = mockDataService.list(Wrappers.<MockData>query().in("group_id", groupIds));
        Map<Integer, List<MockRequest>> requestMap = mockRequests.stream().collect(Collectors.groupingBy(MockRequest::getGroupId));
        Map<Integer, List<MockData>> mockDataMap = mockDataList.stream().collect(Collectors.groupingBy(MockData::getRequestId));
        return groups.stream().map(group -> {
            ExportGroupVo exportGroupVo = new ExportGroupVo();
            copyProperties(exportGroupVo, group);
            List<MockRequest> requests = requestMap.get(group.getId());
            List<ExportRequestVo> exportRequests = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(requests)) {
                exportRequests = requests.stream().map(mockRequest -> {
                    ExportRequestVo exportRequestVo = new ExportRequestVo();
                    copyProperties(exportRequestVo, mockRequest);
                    List<MockData> dataList = mockDataMap.get(mockRequest.getId());
                    List<ExportDataVo> exportDataList = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(dataList)) {
                        exportDataList = dataList.stream().map(mockData -> {
                            ExportDataVo exportMockDataVo = new ExportDataVo();
                            copyProperties(exportMockDataVo, mockData);
                            return exportMockDataVo;
                        }).collect(Collectors.toList());
                    }
                    exportRequestVo.setDataList(exportDataList);
                    return exportRequestVo;
                }).collect(Collectors.toList());
            }
            exportGroupVo.setRequests(exportRequests);
            return exportGroupVo;
        }).collect(Collectors.toList());
    }

    @Override
    public SimpleResult<Integer> doImport(MultipartFile file, MockGroupImportParamVo importVo) {
        try {
            String fileData = StreamUtils.copyToString(file.getInputStream(), StandardCharsets.UTF_8);
            MockGroupImporter importer = MockGroupImporter.findImporter(mockGroupImporters, importVo.getType());
            ExportMockVo mockVo;
            if (importer == null || (mockVo = importer.doImport(fileData)) == null) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2003, 0);
            }
            List<ExportGroupVo> importGroups = mockVo.getGroups();
            List<MockGroup> existGroups = checkGroupsExists(importGroups);
            if (CollectionUtils.isNotEmpty(existGroups)) {
                Integer duplicateStrategy = Objects.requireNonNullElse(importVo.getDuplicateStrategy(), MockConstants.IMPORT_STRATEGY_ERROR);
                if(MockConstants.IMPORT_STRATEGY_ERROR.equals(duplicateStrategy)) {
                    return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2004, 0);
                } else if(MockConstants.IMPORT_STRATEGY_SKIP.equals(duplicateStrategy)) {
                    Set<String> existGroupPaths = existGroups.stream().map(MockGroup::getGroupPath).collect(Collectors.toSet());
                    importGroups = importGroups.stream().filter(group -> !existGroupPaths.contains(group.getGroupPath())).collect(Collectors.toList());
                } else if(MockConstants.IMPORT_STRATEGY_NEW.equals(duplicateStrategy)) {
                    Set<String> existGroupPaths = existGroups.stream().map(MockGroup::getGroupPath).collect(Collectors.toSet());
                    importGroups.forEach(group -> {
                        if (existGroupPaths.contains(group.getGroupPath())) {
                            group.setGroupPath(SimpleMockUtils.uuid());
                        }
                    });
                }
            }
            // 保存数据
            importGroups.forEach(group -> {
                group.setId(null);
                group.setUserName(SecurityUtils.getUserName(importVo.getUserName()));
                group.setGroupPath(StringUtils.defaultIfBlank(StringUtils.trimToEmpty(group.getGroupPath()), SimpleMockUtils.uuid()));
                group.setProjectCode(StringUtils.defaultIfBlank(importVo.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT));
                boolean saved = saveOrUpdate(SimpleMockUtils.addAuditInfo(group));
                if (saved && group.getRequests() != null) {
                    group.getRequests().forEach(request -> {
                        request.setId(null);
                        request.setGroupId(group.getId());
                        boolean reqSaved = mockRequestService.saveOrUpdate(SimpleMockUtils.addAuditInfo(request));
                        if (reqSaved && request.getDataList()!=null) {
                            request.getDataList().forEach(data -> {
                                data.setId(null);
                                data.setGroupId(group.getId());
                                data.setRequestId(request.getId());
                                mockDataService.saveOrUpdate(SimpleMockUtils.addAuditInfo(data));
                            });
                        }
                    });
                }
            });
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_0, importGroups.size());
        } catch (IOException e) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2003,0);
        }
    }

    /**
     * 是否已经存在判断
     * @param groups
     * @return
     */
    protected List<MockGroup> checkGroupsExists(List<? extends MockGroup> groups) {
        List<String> pathList = groups.stream().map(MockGroup::getGroupPath).collect(Collectors.toList());
        return list(Wrappers.<MockGroup>query().in("group_path", pathList));
    }

    protected void copyProperties(Object target, Object source) {
        try {
            BeanUtils.copyProperties(target, source);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("复制属性错误", e);
        }
    }
}
