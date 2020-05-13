package com.mengstudy.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengstudy.simple.mock.contants.MockErrorConstants;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.entity.mock.MockRequest;
import com.mengstudy.simple.mock.service.mock.MockRequestService;
import com.mengstudy.simple.mock.utils.SimpleMockUtils;
import com.mengstudy.simple.mock.utils.SimpleResultUtils;
import com.mengstudy.simple.mock.web.vo.SimpleResult;
import com.mengstudy.simple.mock.web.vo.query.MockRequestQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created on 2020/5/3 21:22 .<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/requests")
public class MockRequestController {

    @Autowired
    private MockRequestService mockRequestService;

    @GetMapping
    public SimpleResult<List<MockRequest>> search(@ModelAttribute MockRequestQueryVo queryVo) {
        Page<MockRequest> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockRequest> queryWrapper = Wrappers.<MockRequest>query();
        if (queryVo.getGroupId() != null) {
            queryWrapper.eq("group_id", queryVo.getGroupId());
        }
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like("request_name", keyword)
                    .or().like("request_path", keyword));
        }
        queryWrapper.orderByAsc("request_path");
        return SimpleResultUtils.createSimpleResult(mockRequestService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockRequest> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.getById(id));
    }

    @GetMapping("/getDefaultData/{requestId}")
    public SimpleResult<MockData> getDefaultData(@PathVariable("requestId") Integer requestId) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.findMockData(requestId, null));
    }

    @PostMapping("/saveMockParams")
    public SimpleResult saveMockParams(@RequestBody MockData data) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.saveMockParams(data));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.deleteMockRequest(id));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockRequest request) {
        if (mockRequestService.existsMockRequest(request)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
        }
        return SimpleResultUtils.createSimpleResult(mockRequestService.saveOrUpdate(SimpleMockUtils.addAuditInfo(request)));
    }
}
