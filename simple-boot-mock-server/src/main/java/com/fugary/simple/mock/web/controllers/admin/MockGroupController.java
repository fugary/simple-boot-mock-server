package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.*;
import com.fugary.simple.mock.service.mock.MockGroupService;
import com.fugary.simple.mock.service.mock.MockLogService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockScenarioService;
import com.fugary.simple.mock.utils.AsyncUtils;
import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.export.ExportGroupVo;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import com.fugary.simple.mock.web.vo.query.MockGroupExportParamVo;
import com.fugary.simple.mock.web.vo.query.MockGroupImportParamVo;
import com.fugary.simple.mock.web.vo.query.MockGroupQueryVo;
import com.fugary.simple.mock.web.vo.query.MockHistoryVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;
import static com.fugary.simple.mock.utils.security.SecurityUtils.getLoginUser;

/**
 * Created on 2020/5/3 21:22 .<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/groups")
public class MockGroupController {

    @Autowired
    private MockGroupService mockGroupService;

    @Autowired
    private MockProjectService mockProjectService;

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockLogService mockLogService;

    @Autowired
    private MockScenarioService mockScenarioService;

    @Autowired
    @Qualifier("asyncQueryThreadPool")
    private ExecutorService asyncQueryThreadPool;

    protected boolean checkGroupAuthority(Integer groupId, String authority) {
        if (groupId == null) {
            return false;
        }
        MockGroup group = mockGroupService.getById(groupId);
        if (group == null) {
            return false;
        }
        return mockProjectService.hasProjectAuthority(group.getUserName(), group.getProjectId(),
                group.getProjectCode(), authority);
    }

    @GetMapping
    public SimpleResult<List<MockGroup>> search(@ModelAttribute MockGroupQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        String queryUserName = queryVo.getUserName();
        Integer projectId = queryVo.getProjectId();
        String projectCode = StringUtils.trimToNull(queryVo.getProjectCode());
        QueryWrapper<MockGroup> queryWrapper = buildGroupQuery(queryVo.getStatus(), keyword);
        String loginUserName = SecurityUtils.getLoginUserName();
        MockProject mockProject = resolveTargetProject(queryUserName, projectId, projectCode);
        if (mockProject != null && !isDefaultProjectCode(mockProject.getProjectCode())) {
            if (!hasProjectReadAccess(mockProject, queryVo.isPublicFlag())) {
                Page<MockGroup> emptyPage = new Page<>(page.getCurrent(), page.getSize());
                return SimpleResultUtils.createSimpleResult(emptyPage)
                        .addInfo("mockProject", null)
                        .addInfo("scenarioMap", new HashMap<>())
                        .addInfo("historyCountMap", new HashMap<>())
                        .addInfo("countMap", new HashMap<>())
                        .addInfo("accessDateMap", new HashMap<>());
            }
            queryWrapper.eq("user_name", mockProject.getUserName());
            appendProjectFilter(queryWrapper, mockProject, mockProject.getId(), mockProject.getProjectCode());
            applyHasRequestFilter(queryWrapper, queryVo.getHasRequest());
            return queryGroupPage(page, queryWrapper, mockProject, queryVo);
        }
        if (mockProject == null && !queryVo.isPublicFlag()
                && (SecurityUtils.isCurrentUser(queryUserName) || SecurityUtils.isAdminUser())
                && projectId == null && StringUtils.isBlank(projectCode)) {
            mockProject = mockProjectService.loadMockProject(queryUserName, MockConstants.MOCK_DEFAULT_PROJECT);
        }
        String userName = SecurityUtils.getUserName(queryUserName);
        boolean noProjectSelected = projectId == null && StringUtils.isBlank(projectCode);
        if (!queryVo.isPublicFlag() && noProjectSelected
                && StringUtils.isNotBlank(userName)
                && StringUtils.equals(userName, loginUserName)
                && !SecurityUtils.isAdminUser()) {
            appendAccessibleProjectsScope(queryWrapper, userName);
            applyHasRequestFilter(queryWrapper, queryVo.getHasRequest());
            return queryGroupPage(page, queryWrapper, mockProject, queryVo);
        }
        if (StringUtils.isBlank(userName) && queryVo.isPublicFlag()
                && mockProject != null && mockProject.isEnabled() && Boolean.TRUE.equals(mockProject.getPublicFlag())) {
            userName = queryUserName; // 鍏佽鏌ヨ
        }
        queryWrapper.eq("user_name", userName);
        if (StringUtils.isBlank(userName) && mockProject != null) {
            if (StringUtils.isNotBlank(loginUserName)
                    && mockProjectService.hasProjectAuthority(mockProject.getUserName(), mockProject.getId(),
                    mockProject.getProjectCode(), MockConstants.AUTHORITY_READABLE)) {
                userName = mockProject.getUserName();
                queryWrapper = buildGroupQuery(queryVo.getStatus(), keyword);
                queryWrapper.eq("user_name", userName);
            }
        }
        boolean emptyProject = StringUtils.isNotBlank(userName) && projectId == null && StringUtils.isBlank(projectCode);
        if (!emptyProject) {
            appendProjectFilter(queryWrapper, mockProject, projectId, projectCode);
        }
        applyHasRequestFilter(queryWrapper, queryVo.getHasRequest());
        return queryGroupPage(page, queryWrapper, mockProject, queryVo);
    }

    private SimpleResult<List<MockGroup>> queryGroupPage(Page<MockGroup> page, QueryWrapper<MockGroup> queryWrapper,
            MockProject mockProject, MockGroupQueryVo queryVo) {
        queryWrapper.orderByDesc("id");
        boolean isExport = queryVo instanceof MockGroupExportParamVo;
        Page<MockGroup> pageResult = mockGroupService.page(page, queryWrapper);
        Future<Map<Integer, Long>> countMapFuture = null;
        if (!isExport && CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            countMapFuture = asyncQueryThreadPool.submit(() -> {
                List<Integer> groupIds = pageResult.getRecords().stream().map(MockGroup::getId)
                        .collect(Collectors.toList());
                QueryWrapper<MockRequest> countQuery = Wrappers.<MockRequest>query()
                        .select("group_id as group_key", "count(0) as data_count")
                        .in("group_id", groupIds).isNull(DB_MODIFY_FROM_KEY)
                        .groupBy("group_id");
                return mockRequestService.listMaps(countQuery).stream().map(CountData::new)
                        .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                                CountData::getDataCount));
            });
        }
        Future<Map<String, Date>> accessDateMapFuture = null;
        if (!isExport && CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            accessDateMapFuture = asyncQueryThreadPool.submit(() -> {
                List<String> groupPaths = pageResult.getRecords().stream().map(MockGroup::getGroupPath)
                        .collect(Collectors.toList());
                QueryWrapper<MockLog> countQuery = Wrappers.<MockLog>query()
                        .select("mock_group_path as group_key", "MAX(create_date) AS group_value")
                        .in("mock_group_path", groupPaths)
                        .groupBy("mock_group_path");
                return mockLogService.listMaps(countQuery).stream().map(map -> new GroupByData<>(map, Date.class))
                        .collect(Collectors.toMap(GroupByData::getGroupKey, GroupByData::getGroupValue));
            });
        }
        Future<Map<Integer, Long>> historyCountMapFuture = null;
        if (!isExport && CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            historyCountMapFuture = asyncQueryThreadPool.submit(() -> {
                List<Integer> groupIds = pageResult.getRecords().stream().map(MockGroup::getId)
                        .collect(Collectors.toList());
                QueryWrapper<MockGroup> historyCountQuery = Wrappers.<MockGroup>query()
                        .select("modify_from as group_key", "count(0) as data_count")
                        .in("modify_from", groupIds)
                        .groupBy("modify_from");
                return mockGroupService.listMaps(historyCountQuery).stream().map(CountData::new)
                        .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                                CountData::getDataCount));
            });
        }
        Map<Integer, List<MockScenario>> scenarioMap = loadScenarioMap(pageResult.getRecords());
        return SimpleResultUtils.createSimpleResult(pageResult)
                .addInfo("mockProject", mockProject)
                .addInfo("scenarioMap", scenarioMap)
                .addInfo("historyCountMap", AsyncUtils.get(historyCountMapFuture, HashMap::new))
                .addInfo("countMap", AsyncUtils.get(countMapFuture, HashMap::new))
                .addInfo("accessDateMap", AsyncUtils.get(accessDateMapFuture, HashMap::new));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockGroup> get(@PathVariable("id") Integer id) {
        MockGroup mockGroup = mockGroupService.getById(id);
        MockProject mockProject = null;
        if (mockGroup != null) {
            mockProject = mockProjectService.loadMockProject(mockGroup.getUserName(), mockGroup.getProjectId(),
                    mockGroup.getProjectCode());
        }
        if (mockGroup == null || mockProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!Boolean.TRUE.equals(mockProject.getPublicFlag())
                && !checkGroupAuthority(id, MockConstants.AUTHORITY_READABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockGroup)
                .addInfo("mockProject", mockProject);
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        if (!checkGroupAuthority(id, MockConstants.AUTHORITY_DELETABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockGroupService.deleteMockGroup(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult<Object> removeByIds(@PathVariable("ids") List<Integer> ids) {
        if (ids != null) {
            for (Integer id : ids) {
                if (!checkGroupAuthority(id, MockConstants.AUTHORITY_DELETABLE)) {
                    return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
                }
            }
        }
        return SimpleResultUtils.createSimpleResult(mockGroupService.deleteMockGroups(ids));
    }

    @PostMapping
    public SimpleResult<MockGroup> save(@RequestBody MockGroup group) {
        if (StringUtils.isBlank(group.getGroupPath())) {
            group.setGroupPath(SimpleMockUtils.uuid());
        }
        MockGroup existGroup = null;
        if (group.getId() != null) {
            existGroup = mockGroupService.getById(group.getId());
            if (existGroup == null) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
            }
            if (StringUtils.isBlank(group.getUserName())) {
                group.setUserName(existGroup.getUserName());
            }
            if (!SecurityUtils.validateUserUpdate(existGroup.getUserName())) {
                boolean sourceWritable = mockProjectService.hasProjectAuthority(existGroup.getUserName(),
                        existGroup.getProjectId(), existGroup.getProjectCode(), MockConstants.AUTHORITY_WRITABLE);
                if (!sourceWritable) {
                    return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
                }
                boolean projectChanged = (group.getProjectId() != null && !Objects.equals(group.getProjectId(), existGroup.getProjectId()))
                        || (StringUtils.isNotBlank(group.getProjectCode())
                        && !StringUtils.equals(StringUtils.trimToEmpty(group.getProjectCode()), StringUtils.trimToEmpty(existGroup.getProjectCode())))
                        || (StringUtils.isNotBlank(group.getUserName())
                        && !StringUtils.equals(group.getUserName(), existGroup.getUserName()));
                if (!projectChanged) {
                    group.setUserName(existGroup.getUserName());
                    group.setProjectId(existGroup.getProjectId());
                    group.setProjectCode(existGroup.getProjectCode());
                }
            }
        }
        if (mockGroupService.existsMockGroup(group)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        MockUser loginUser = getLoginUser();
        if (StringUtils.isBlank(group.getUserName()) && loginUser != null) {
            group.setUserName(loginUser.getUserName());
        }
        MockProject project = resolveTargetProject(group.getUserName(), group.getProjectId(), group.getProjectCode());
        applyProjectRelation(group, project);
        if (!mockProjectService.hasProjectAuthority(group.getUserName(), group.getProjectId(),
                group.getProjectCode(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return mockGroupService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(group));
    }

    @PostMapping("/copyMockGroup/{groupId}")
    public SimpleResult<MockGroup> copyMockGroup(@PathVariable("groupId") String groupIdsStr,
            @RequestBody MockGroupQueryVo copyVo) {
        MockProject existsProject = resolveTargetProject(copyVo.getUserName(), copyVo.getProjectId(), copyVo.getProjectCode());
        if (existsProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (MockConstants.MOCK_DEFAULT_PROJECT.equals(existsProject.getProjectCode())) {
            existsProject.setUserName(copyVo.getUserName());
        }
        String[] groupIds = groupIdsStr.split("\\s*,\\s*");
        SimpleResult<MockGroup> result = null;
        for (String groupId : groupIds) {
            result = mockGroupService.copyMockGroup(NumberUtils.toInt(groupId), existsProject);
        }
        return result;
    }

    @PostMapping("/import")
    public SimpleResult<Integer> doImport(@ModelAttribute MockGroupImportParamVo importVo,
            MultipartHttpServletRequest request) {
        List<MultipartFile> files = SimpleMockUtils.getUploadFiles(request);
        if (files.isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2002, 0);
        }
        SimpleResult<List<ExportGroupVo>> importGroupsResult = mockGroupService.toImportGroups(files, importVo);
        if (importGroupsResult.isSuccess()) {
            MockProject project = resolveTargetProject(importVo.getUserName(), importVo.getProjectId(), importVo.getProjectCode());
            if (project != null) {
                if (StringUtils.equals(project.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT)) {
                    importVo.setProjectId(null);
                    importVo.setProjectCode(MockConstants.MOCK_DEFAULT_PROJECT);
                } else {
                    importVo.setProjectId(project.getId());
                    importVo.setProjectCode(project.getProjectCode());
                    importVo.setUserName(project.getUserName());
                }
            } else {
                importVo.setProjectId(null);
                importVo.setProjectCode(MockConstants.MOCK_DEFAULT_PROJECT);
            }
            return mockGroupService.importGroups(importGroupsResult.getResultData(), importVo);
        }
        return SimpleResultUtils.createSimpleResult(importGroupsResult.getCode(), 0);
    }

    @PostMapping("/checkExport")
    public SimpleResult checkExport(@RequestBody MockGroupExportParamVo queryVo) {
        List<MockGroup> groups = loadExportGroups(queryVo);
        return SimpleResultUtils.createSimpleResult(!groups.isEmpty());
    }

    private List<MockGroup> loadExportGroups(MockGroupExportParamVo queryVo) {
        List<MockGroup> groups = new ArrayList<>();
        if (queryVo.isExportAll()) {
            queryVo.setPage(SimpleResultUtils.getNewPage(1, MockConstants.MAX_EXPORT_COUNT));
            SimpleResult<List<MockGroup>> simpleResult = this.search(queryVo);
            groups = simpleResult.getResultData();
        } else if (CollectionUtils.isNotEmpty(queryVo.getGroupIds())) {
            groups = mockGroupService.listByIds(queryVo.getGroupIds());
        }
        return groups;
    }

    @GetMapping("/export")
    public void export(@ModelAttribute MockGroupExportParamVo queryVo, HttpServletResponse response)
            throws IOException {
        List<MockGroup> groups = loadExportGroups(queryVo);
        List<ExportGroupVo> groupVoList = mockGroupService.loadExportGroups(groups);
        ExportMockVo mockVo = new ExportMockVo();
        mockVo.setGroups(groupVoList);
        String json = JsonUtils.toJson(mockVo);
        String fileName = "mock-groups-"
                + DateFormatUtils.format(System.currentTimeMillis(), MockConstants.OUTPUT_FILE_NAME) + ".json";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        try (InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                OutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            response.getOutputStream().flush();
        }
    }

    @PostMapping("/histories/{id}")
    public SimpleResult<List<MockGroup>> histories(@PathVariable("id") Integer id,
            @RequestBody MockGroupQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .eq("modify_from", id)
                .orderByDesc("data_version");
        Page<MockGroup> pageResult = mockGroupService.page(page, queryWrapper);
        MockGroup current = mockGroupService.getById(id);
        List<MockGroup> allGroups = new ArrayList<>(pageResult.getRecords());
        if (current != null) {
            allGroups.add(current);
        }
        Map<Integer, List<MockScenario>> scenarioMap = loadScenarioMap(allGroups);
        return SimpleResultUtils.createSimpleResult(pageResult)
                .addInfo("current", current)
                .addInfo("scenarioMap", scenarioMap);
    }

    @PostMapping("/loadHistoryDiff")
    public SimpleResult<Map<String, MockGroup>> loadHistoryDiff(@RequestBody MockHistoryVo queryVo) {
        Integer id = queryVo.getId();
        Integer maxVersion = queryVo.getVersion();
        MockGroup modified = mockGroupService.getById(id);
        Page<MockGroup> page = new Page<>(1, 2);
        mockGroupService.page(page, Wrappers.<MockGroup>query()
                .eq(DB_MODIFY_FROM_KEY, ObjectUtils.defaultIfNull(modified.getModifyFrom(), modified.getId()))
                .le(maxVersion != null, "data_version", maxVersion)
                .orderByDesc("data_version"));
        if (page.getRecords().isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        } else {
            Map<String, MockGroup> map = new HashMap<>(2);
            List<MockGroup> dataList = page.getRecords();
            map.put("modified", modified);
            dataList.stream().filter(data -> !data.getId().equals(modified.getId())).findFirst().ifPresent(data -> {
                map.put("original", data);
            });
            List<MockGroup> allGroups = new ArrayList<>(map.values());
            Map<Integer, List<MockScenario>> scenarioMap = loadScenarioMap(allGroups);
            return SimpleResultUtils.createSimpleResult(map)
                    .addInfo("scenarioMap", scenarioMap);
        }
    }

    @PostMapping("/recoverFromHistory")
    public SimpleResult<MockGroup> recoverFromHistory(@RequestBody MockHistoryVo historyVo) {
        MockGroup history = mockGroupService.getById(historyVo.getId());
        MockGroup target = null;
        if (history != null && history.getModifyFrom() != null) {
            target = mockGroupService.getById(history.getModifyFrom());
        }
        if (history == null || target == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!checkGroupAuthority(target.getId(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        SimpleMockUtils.copyFromHistory(history, target);
        return mockGroupService.newSaveOrUpdate(target);
    }

    private Map<Integer, List<MockScenario>> loadScenarioMap(List<MockGroup> groups) {
        Map<Integer, List<MockScenario>> scenarioMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(groups)) {
            Map<Integer, Integer> groupIdToSourceId = new HashMap<>();
            Set<Integer> sourceIds = new HashSet<>();
            for (MockGroup group : groups) {
                if (group.getId() == null) {
                    continue;
                }
                Integer sourceId = (group.getModifyFrom() != null && group.getModifyFrom() > 0)
                        ? group.getModifyFrom()
                        : group.getId();
                groupIdToSourceId.put(group.getId(), sourceId);
                sourceIds.add(sourceId);
            }
            if (!sourceIds.isEmpty()) {
                List<MockScenario> scenarios = mockScenarioService.list(Wrappers.<MockScenario>query()
                        .in("group_id", sourceIds)
                        .select("group_id", "scenario_code", "scenario_name", "status"));
                if (CollectionUtils.isNotEmpty(scenarios)) {
                    Map<Integer, List<MockScenario>> sourceMap = scenarios.stream()
                            .collect(Collectors.groupingBy(MockScenario::getGroupId));
                    groupIdToSourceId.forEach((groupId, sourceId) -> {
                        List<MockScenario> list = sourceMap.get(sourceId);
                        if (list != null) {
                            scenarioMap.put(groupId, list);
                        }
                    });
                }
            }
        }
        return scenarioMap;
    }

    private QueryWrapper<MockGroup> buildGroupQuery(Integer status, String keyword) {
        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .eq(status != null, "status", status)
                .isNull(DB_MODIFY_FROM_KEY);
        queryWrapper.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("group_name", keyword)
                .or().like("group_path", keyword)
                .or().like("proxy_url", keyword)
                .or().like("description", keyword));
        return queryWrapper;
    }

    private void appendProjectFilter(QueryWrapper<MockGroup> queryWrapper, MockProject mockProject,
            Integer projectId, String projectCode) {
        if (projectId != null) {
            if (mockProject != null && !isDefaultProjectCode(mockProject.getProjectCode())) {
                queryWrapper.and(wrapper -> wrapper.eq("project_id", projectId)
                        .or(legacy -> legacy.isNull("project_id")
                                .eq("project_code", mockProject.getProjectCode())));
            } else {
                queryWrapper.eq("project_id", projectId);
            }
            return;
        }
        if (StringUtils.isBlank(projectCode)) {
            return;
        }
        if (mockProject != null && !StringUtils.equals(projectCode, MockConstants.MOCK_DEFAULT_PROJECT)) {
            queryWrapper.and(wrapper -> wrapper.eq("project_id", mockProject.getId())
                    .or(legacy -> legacy.isNull("project_id").eq("project_code", projectCode)));
            return;
        }
        queryWrapper.eq("project_code", projectCode);
    }

    private MockProject resolveTargetProject(String userName, Integer projectId, String projectCode) {
        String normalizedProjectCode = StringUtils.trimToNull(projectCode);
        if (projectId != null || StringUtils.isNotBlank(normalizedProjectCode)) {
            return mockProjectService.loadMockProject(userName, projectId, normalizedProjectCode);
        }
        return null;
    }

    private boolean hasProjectReadAccess(MockProject project, boolean publicFlag) {
        if (project == null || !project.isEnabled()) {
            return false;
        }
        return (publicFlag && Boolean.TRUE.equals(project.getPublicFlag()))
                || mockProjectService.hasProjectAuthority(project.getUserName(), project.getId(),
                project.getProjectCode(), MockConstants.AUTHORITY_READABLE);
    }

    private boolean isDefaultProjectCode(String projectCode) {
        return StringUtils.equalsIgnoreCase(MockConstants.MOCK_DEFAULT_PROJECT, StringUtils.trimToEmpty(projectCode));
    }

    private void applyHasRequestFilter(QueryWrapper<MockGroup> queryWrapper, Boolean hasRequest) {
        if (hasRequest != null) {
            queryWrapper.exists(hasRequest,
                    "select 1 from t_mock_request where t_mock_request.group_id=t_mock_group.id");
            queryWrapper.notExists(!hasRequest,
                    "select 1 from t_mock_request where t_mock_request.group_id=t_mock_group.id");
        }
    }

    private void appendAccessibleProjectsScope(QueryWrapper<MockGroup> queryWrapper, String userName) {
        queryWrapper.and(wrapper -> {
            wrapper.eq("user_name", userName);
            wrapper.or().exists(buildReadableProjectExistsSql(userName));
        });
    }

    private String buildReadableProjectExistsSql(String userName) {
        return "select 1 from t_mock_project p join t_mock_project_user pu "
                + "on (pu.project_id = p.id or (pu.project_id is null and pu.project_code = p.project_code)) "
                + "where pu.user_name = '" + userName + "' and p.status = 1 and p.project_code <> '"
                + MockConstants.MOCK_DEFAULT_PROJECT + "' and (p.id = t_mock_group.project_id "
                + "or (t_mock_group.project_id is null and p.project_code = t_mock_group.project_code))";
    }

    private void applyProjectRelation(MockGroup group, MockProject project) {
        if (project == null || StringUtils.equals(project.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT)) {
            group.setProjectId(null);
            group.setProjectCode(MockConstants.MOCK_DEFAULT_PROJECT);
            if (StringUtils.isBlank(group.getUserName()) || !SecurityUtils.validateUserUpdate(group.getUserName())) {
                MockUser loginUser = getLoginUser();
                if (loginUser != null) {
                    group.setUserName(loginUser.getUserName());
                }
            }
            return;
        }
        group.setProjectId(project.getId());
        group.setProjectCode(project.getProjectCode());
        group.setUserName(project.getUserName());
    }
}
