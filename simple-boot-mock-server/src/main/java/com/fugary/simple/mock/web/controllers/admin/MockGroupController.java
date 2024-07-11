package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.fugary.simple.mock.web.vo.query.MockGroupQueryVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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

    private static final Logger log = LoggerFactory.getLogger(MockGroupController.class);

    @Autowired
    private MockGroupService mockGroupService;

    @GetMapping
    public SimpleResult<List<MockGroup>> search(@ModelAttribute MockGroupQueryVo queryVo) {
        Page<MockGroup> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockGroup> queryWrapper = Wrappers.query();
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("group_name", keyword)
                    .or().like("group_path", keyword));
        }
        String userName = getUserName(queryVo);
        queryWrapper.eq("user_name", userName);
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

    @PostMapping
    public SimpleResult save(@RequestBody MockGroup group) {
        if (StringUtils.isBlank(group.getGroupPath())) {
            group.setGroupPath(SimpleMockUtils.uuid());
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

    @PostMapping("/checkExport")
    public SimpleResult checkExport(@RequestBody MockGroupQueryVo queryVo) {
        List<MockGroup> groups = new ArrayList<>();
        if (queryVo.isExportAll()) {
            groups = mockGroupService.list(Wrappers.<MockGroup>query().in("user_name", getUserName(queryVo)));
        } else if(CollectionUtils.isNotEmpty(queryVo.getGroupIds())){
            groups = mockGroupService.listByIds(queryVo.getGroupIds());
        }
        return SimpleResultUtils.createSimpleResult(!groups.isEmpty());
    }

    @GetMapping("/export")
    public void export(@ModelAttribute MockGroupQueryVo queryVo, HttpServletResponse response) throws IOException {
        List<MockGroup> groups = new ArrayList<>();
        if (queryVo.isExportAll()) {
            groups = mockGroupService.list(Wrappers.<MockGroup>query().in("user_name", getUserName(queryVo)));
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

    protected String getUserName(MockGroupQueryVo queryVo) {
        MockUser loginUser = getLoginUser();
        String userName = StringUtils.defaultIfBlank(queryVo.getUserName(), loginUser != null ? loginUser.getUserName() : "");
        userName = SecurityUtils.validateUserUpdate(userName) ? userName : "";
        return userName;
    }
}
