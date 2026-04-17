package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.service.mock.MockLogService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockLogQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Create date 2024/11/14<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping("/admin/logs")
public class MockLogController {

    @Autowired
    private MockLogService mockLogService;

    @GetMapping
    public SimpleResult<List<MockLog>> search(@ModelAttribute MockLogQueryVo queryVo) {
        Page<MockLog> page = SimpleResultUtils.toPage(queryVo);
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        QueryWrapper<MockLog> queryWrapper = Wrappers.<MockLog>query()
                .eq(StringUtils.isNotBlank(queryVo.getUserName()), "user_name", queryVo.getUserName())
                .eq(StringUtils.isNotBlank(queryVo.getLogType()), "log_type", StringUtils.trimToEmpty(queryVo.getLogType()))
                .eq(StringUtils.isNotBlank(queryVo.getLogResult()), "log_result", StringUtils.trimToEmpty(queryVo.getLogResult()))
                .eq(StringUtils.isNotBlank(queryVo.getIpAddress()), "ip_address", StringUtils.trimToEmpty(queryVo.getIpAddress()))
                .eq(StringUtils.isNotBlank(queryVo.getMockGroupPath()), "mock_group_path", StringUtils.trimToEmpty(queryVo.getMockGroupPath()))
                .ge(queryVo.getStartDate() != null, "create_date", queryVo.getStartDate())
                .le(queryVo.getEndDate() != null, "create_date", queryVo.getEndDate())
                .like(StringUtils.isNotBlank(queryVo.getLogName()), "log_name", StringUtils.trimToEmpty(queryVo.getLogName()))
                .and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("log_message", keyword)
                        .or().like("log_data", keyword)
                        .or().like("exceptions", keyword));
        if (!SecurityUtils.isAdminUser()) {
            String loginUserName = StringUtils.trimToNull(SecurityUtils.getLoginUserName());
            if (loginUserName == null) {
                return createEmptyLogResult(page);
            }
            appendAccessibleLogsScope(queryWrapper, loginUserName);
        }
        queryWrapper.orderByDesc("id");
        return SimpleResultUtils.createSimpleResult(mockLogService.page(page, queryWrapper));
    }

    private SimpleResult<List<MockLog>> createEmptyLogResult(Page<MockLog> page) {
        return SimpleResultUtils.createSimpleResult(new Page<>(page.getCurrent(), page.getSize()));
    }

    private void appendAccessibleLogsScope(QueryWrapper<MockLog> queryWrapper, String userName) {
        queryWrapper.and(wrapper -> wrapper.eq("user_name", userName)
                .or().exists(buildReadableProjectExistsSql(userName)));
    }

    private String buildReadableProjectExistsSql(String userName) {
        String safeUserName = StringUtils.replace(StringUtils.trimToEmpty(userName), "'", "''");
        return "select 1 from t_mock_group g join t_mock_project p "
                + "on (g.project_id = p.id or (g.project_id is null and g.project_code = p.project_code)) "
                + "where g.group_path = t_mock_log.mock_group_path and g.modify_from is null "
                + "and p.project_code <> '" + MockConstants.MOCK_DEFAULT_PROJECT + "' and (p.user_name = '"
                + safeUserName + "' or exists (select 1 from t_mock_project_user pu where pu.user_name = '"
                + safeUserName + "' and (pu.project_id = p.id or (pu.project_id is null and pu.project_code = p.project_code)) "
                + "and pu.authorities like '%" + MockConstants.AUTHORITY_READABLE + "%'))";
    }
}
