package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockGroup;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.service.mock.MockGroupService;
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
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping
    public SimpleResult<List<MockGroup>> search(@ModelAttribute MockGroupQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        QueryWrapper<MockGroup> queryWrapper = Wrappers.<MockGroup>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus());
        queryWrapper.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("group_name", keyword)
                    .or().like("group_path", keyword)
                    .or().like("description", keyword));
        String userName = SecurityUtils.getUserName(queryVo.getUserName());
        queryWrapper.eq("user_name", userName);
        queryWrapper.eq("project_code", StringUtils.defaultIfBlank(queryVo.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT));
        return SimpleResultUtils.createSimpleResult(mockGroupService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockGroup> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockGroupService.getById(id));
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
    public SimpleResult<MockGroup> copyMockGroup(@PathVariable("groupId") Integer id) {
        return mockGroupService.copyMockGroup(id, null);
    }

    @PostMapping("/import")
    public SimpleResult<Integer> doImport(@ModelAttribute MockGroupImportParamVo importVo, MultipartHttpServletRequest request){
        List<MultipartFile> files = SimpleMockUtils.getUploadFiles(request);
        if (files.isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_2002, 0);
        }
        SimpleResult<List<ExportGroupVo>> importGroupsResult = mockGroupService.toImportGroups(files.get(0), importVo);
        if (importGroupsResult.isSuccess()) {
            return mockGroupService.importGroups(importGroupsResult.getResultData(), importVo);
        }
        return SimpleResultUtils.createSimpleResult(importGroupsResult.getCode(), 0);
    }

    @PostMapping("/checkExport")
    public SimpleResult checkExport(@RequestBody MockGroupExportParamVo queryVo) {
        List<MockGroup> groups = new ArrayList<>();
        if (queryVo.isExportAll()) {
            groups = mockGroupService.list(Wrappers.<MockGroup>query().eq("user_name", SecurityUtils.getUserName(queryVo.getUserName()))
                    .eq("project_code", StringUtils.defaultIfBlank(queryVo.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT)));
        } else if(CollectionUtils.isNotEmpty(queryVo.getGroupIds())){
            groups = mockGroupService.listByIds(queryVo.getGroupIds());
        }
        return SimpleResultUtils.createSimpleResult(!groups.isEmpty());
    }

    @GetMapping("/export")
    public void export(@ModelAttribute MockGroupExportParamVo queryVo, HttpServletResponse response) throws IOException {
        List<MockGroup> groups = new ArrayList<>();
        if (queryVo.isExportAll()) {
            groups = mockGroupService.list(Wrappers.<MockGroup>query().in("user_name", SecurityUtils.getUserName(queryVo.getUserName()))
                    .eq("project_code", StringUtils.defaultIfBlank(queryVo.getProjectCode(), MockConstants.MOCK_DEFAULT_PROJECT)));
        } else if(CollectionUtils.isNotEmpty(queryVo.getGroupIds())){
            groups = mockGroupService.listByIds(queryVo.getGroupIds());
        }
        List<ExportGroupVo> groupVoList = mockGroupService.loadExportGroups(groups);
        ExportMockVo mockVo = new ExportMockVo();
        mockVo.setGroups(groupVoList);
        String json = JsonUtils.toJson(mockVo);
        String fileName = "mock-groups-" + System.currentTimeMillis() + ".json";
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
