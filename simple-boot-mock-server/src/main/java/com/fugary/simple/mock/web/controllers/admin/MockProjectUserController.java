package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.MockProject;
import com.fugary.simple.mock.entity.mock.MockProjectUser;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockProjectUserService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 协作成员管理
 * Create date 2026/03/30
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/projectUsers")
public class MockProjectUserController {

    @Autowired
    private MockProjectUserService mockProjectUserService;

    @Autowired
    private MockProjectService mockProjectService;

    /**
     * 查询某项目的协作成员列表
     *
     * @param projectCode
     * @return
     */
    @GetMapping
    public SimpleResult<List<MockProjectUser>> search(@RequestParam("projectCode") String projectCode) {
        return SimpleResultUtils.createSimpleResult(mockProjectUserService.loadProjectUsers(projectCode));
    }

    /**
     * 保存协作成员
     *
     * @param projectUser
     * @return
     */
    @PostMapping
    public SimpleResult<MockProjectUser> save(@RequestBody MockProjectUser projectUser) {
        if (StringUtils.isBlank(projectUser.getProjectCode()) || StringUtils.isBlank(projectUser.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_400, null);
        }
        // 只有项目的 owner 或者 admin 才能管理协作成员
        MockProject project = mockProjectService.getOne(Wrappers.<MockProject>query()
                .eq("project_code", projectUser.getProjectCode()));
        if (project == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, null);
        }
        if (!SecurityUtils.validateUserUpdate(project.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, null);
        }
        // 检查是否已存在
        if (projectUser.getId() == null) {
            boolean exists = mockProjectUserService.exists(Wrappers.<MockProjectUser>query()
                    .eq("project_code", projectUser.getProjectCode())
                    .eq("user_name", projectUser.getUserName()));
            if (exists) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001, null);
            }
        }
        if (StringUtils.isBlank(projectUser.getCreator()) && SecurityUtils.getLoginUser() != null) {
            projectUser.setCreator(SecurityUtils.getLoginUserName());
        }
        SimpleMockUtils.addAuditInfo(projectUser);
        mockProjectUserService.saveOrUpdate(projectUser);
        return SimpleResultUtils.createSimpleResult(projectUser);
    }

    /**
     * 删除协作成员
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public SimpleResult<Boolean> remove(@PathVariable("id") Integer id) {
        MockProjectUser projectUser = mockProjectUserService.getById(id);
        if (projectUser == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, false);
        }
        MockProject project = mockProjectService.getOne(Wrappers.<MockProject>query()
                .eq("project_code", projectUser.getProjectCode()));
        if (project == null || !SecurityUtils.validateUserUpdate(project.getUserName())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, false);
        }
        return SimpleResultUtils.createSimpleResult(mockProjectUserService.removeById(id));
    }

}
