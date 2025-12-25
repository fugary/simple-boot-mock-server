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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping
    public SimpleResult<List<MockGroup>> search(@ModelAttribute MockGroupQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        String queryUserName = queryVo.getUserName();
        String projectCode = queryVo.getProjectCode();
        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus());
        queryWrapper.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("group_name", keyword)
                .or().like("group_path", keyword)
                .or().like("proxy_url", keyword)
                .or().like("description", keyword));
        MockProject mockProject = mockProjectService.loadMockProject(queryUserName, projectCode);
        if (mockProject == null && !queryVo.isPublicFlag()
                && (SecurityUtils.isCurrentUser(queryUserName) || SecurityUtils.isAdminUser())
                && StringUtils.isBlank(projectCode)) {
            mockProject = mockProjectService.loadMockProject(queryUserName, MockConstants.MOCK_DEFAULT_PROJECT);
        }
        String userName = SecurityUtils.getUserName(queryUserName);
        if (StringUtils.isBlank(userName) && queryVo.isPublicFlag()
                && mockProject != null && mockProject.isEnabled() && Boolean.TRUE.equals(mockProject.getPublicFlag())) {
            userName = queryUserName; // 允许查询
        }
        queryWrapper.eq("user_name", userName);
        boolean emptyProjectCode = StringUtils.isNotBlank(userName) && StringUtils.isBlank(projectCode);
        if (!emptyProjectCode) {
            queryWrapper.eq("project_code", projectCode);
        }
        if (queryVo.getHasRequest() != null) {
            queryWrapper.exists(queryVo.getHasRequest(),
                    "select 1 from t_mock_request where t_mock_request.group_id=t_mock_group.id");
            queryWrapper.notExists(!queryVo.getHasRequest(),
                    "select 1 from t_mock_request where t_mock_request.group_id=t_mock_group.id");
        }
        boolean isExport = queryVo instanceof MockGroupExportParamVo;
        Page<MockGroup> pageResult = mockGroupService.page(page, queryWrapper);
        Map<Integer, Long> countMap = new HashMap<>();
        if (!isExport && CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> groupIds = pageResult.getRecords().stream().map(MockGroup::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockRequest> countQuery = Wrappers.<MockRequest>query()
                    .select("group_id as group_key", "count(0) as data_count")
                    .in("group_id", groupIds).isNull(DB_MODIFY_FROM_KEY)
                    .groupBy("group_id");
            countMap = mockRequestService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        Map<String, Date> accessDateMap = new HashMap<>();
        if (!isExport && CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<String> groupPaths = pageResult.getRecords().stream().map(MockGroup::getGroupPath)
                    .collect(Collectors.toList());
            QueryWrapper<MockLog> countQuery = Wrappers.<MockLog>query()
                    .select("mock_group_path as group_key", "MAX(create_date) AS group_value")
                    .in("mock_group_path", groupPaths)
                    .groupBy("mock_group_path");
            accessDateMap = mockLogService.listMaps(countQuery).stream().map(map -> new GroupByData<>(map, Date.class))
                    .collect(Collectors.toMap(GroupByData::getGroupKey, GroupByData::getGroupValue));
        }
        return SimpleResultUtils.createSimpleResult(pageResult)
                .addInfo("mockProject", mockProject)
                .addInfo("countMap", countMap)
                .addInfo("accessDateMap", accessDateMap);
    }

    @GetMapping("/{id}")
    public SimpleResult<MockGroup> get(@PathVariable("id") Integer id) {
        MockGroup mockGroup = mockGroupService.getById(id);
        MockProject mockProject = null;
        if (mockGroup != null) {
            mockProject = mockProjectService.loadMockProject(mockGroup.getUserName(), mockGroup.getProjectCode());
        }
        if (mockGroup == null || mockProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!Boolean.TRUE.equals(mockProject.getPublicFlag()) && !SecurityUtils.validateUserUpdate(mockGroup.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockGroup)
                .addInfo("mockProject", mockProject);
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockGroupService.deleteMockGroup(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockGroupService.deleteMockGroups(ids));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockGroup group) {
        if (StringUtils.isBlank(group.getGroupPath())) {
            group.setGroupPath(SimpleMockUtils.uuid());
        }
        if (StringUtils.isBlank(group.getProjectCode())) {
            group.setProjectCode(MockConstants.MOCK_DEFAULT_PROJECT);
        }
        if (mockGroupService.existsMockGroup(group)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        MockUser loginUser = getLoginUser();
        if (StringUtils.isBlank(group.getUserName()) && loginUser != null) {
            group.setUserName(loginUser.getUserName());
        }
        if (!SecurityUtils.validateUserUpdate(group.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockGroupService.saveOrUpdate(SimpleMockUtils.addAuditInfo(group)));
    }

    @PostMapping("/copyMockGroup/{groupId}")
    public SimpleResult<MockGroup> copyMockGroup(@PathVariable("groupId") Integer id,
                                                 @RequestBody MockGroupQueryVo copyVo) {
        MockProject existsProject = mockProjectService.loadMockProject(copyVo.getUserName(), copyVo.getProjectCode());
        if (existsProject == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (MockConstants.MOCK_DEFAULT_PROJECT.equals(existsProject.getProjectCode())) {
            existsProject.setUserName(copyVo.getUserName());
        }
        return mockGroupService.copyMockGroup(id, existsProject);
    }

    @PostMapping("/import")
    public SimpleResult<Integer> doImport(@ModelAttribute MockGroupImportParamVo importVo, MultipartHttpServletRequest request){
        List<MultipartFile> files = SimpleMockUtils.getUploadFiles(request);
        if (files.isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2002, 0);
        }
        SimpleResult<List<ExportGroupVo>> importGroupsResult = mockGroupService.toImportGroups(files, importVo);
        if (importGroupsResult.isSuccess()) {
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
        } else if(CollectionUtils.isNotEmpty(queryVo.getGroupIds())){
            groups = mockGroupService.listByIds(queryVo.getGroupIds());
        }
        return groups;
    }

    @GetMapping("/export")
    public void export(@ModelAttribute MockGroupExportParamVo queryVo, HttpServletResponse response) throws IOException {
        List<MockGroup> groups = loadExportGroups(queryVo);
        List<ExportGroupVo> groupVoList = mockGroupService.loadExportGroups(groups);
        ExportMockVo mockVo = new ExportMockVo();
        mockVo.setGroups(groupVoList);
        String json = JsonUtils.toJson(mockVo);
        String fileName = "mock-groups-" + DateFormatUtils.format(System.currentTimeMillis(), MockConstants.OUTPUT_FILE_NAME) + ".json";
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        try(InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                OutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            response.getOutputStream().flush();
        }
    }
}
