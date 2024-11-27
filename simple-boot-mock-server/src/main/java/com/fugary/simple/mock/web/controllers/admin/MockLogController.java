package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.MockLog;
import com.fugary.simple.mock.service.mock.MockLogService;
import com.fugary.simple.mock.utils.SimpleResultUtils;
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
                .ge(queryVo.getStartDate() != null, "create_date", queryVo.getStartDate())
                .le(queryVo.getEndDate() != null, "create_date", queryVo.getEndDate())
                .like(StringUtils.isNotBlank(queryVo.getLogName()), "log_name", StringUtils.trimToEmpty(queryVo.getLogName()))
                .and(StringUtils.isNotBlank(keyword), wrapper -> wrapper.like("log_message", keyword)
                        .or().like("log_data", keyword)
                        .or().like("exceptions", keyword))
                .orderByDesc("id");
        return SimpleResultUtils.createSimpleResult(mockLogService.page(page, queryWrapper));
    }
}
