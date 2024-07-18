package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockRequestQueryVo;
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
            queryWrapper.and(wrapper -> wrapper.like("request_path", keyword));
        }
        if (StringUtils.isNotBlank(queryVo.getMethod())) {
            queryWrapper.and(wrapper -> wrapper.eq("method", queryVo.getMethod()));
        }
        queryWrapper.orderByAsc("request_path", "method");
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

    @PostMapping("/copyMockRequest/{requestId}")
    public SimpleResult copyMockRequest(@PathVariable("requestId") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.copyMockRequest(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.deleteMockRequest(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.removeByIds(ids));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockRequest request) {
//        if (mockRequestService.existsMockRequest(request)) { // 不再校验重复，可以随便添加
//            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1001);
//        }
        return SimpleResultUtils.createSimpleResult(mockRequestService.saveOrUpdate(SimpleMockUtils.addAuditInfo(request)));
    }
}
