package com.mengstudy.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengstudy.simple.mock.entity.mock.MockData;
import com.mengstudy.simple.mock.service.mock.MockDataService;
import com.mengstudy.simple.mock.utils.SimpleResultUtils;
import com.mengstudy.simple.mock.web.vo.SimpleResult;
import com.mengstudy.simple.mock.web.vo.query.MockDataQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created on 2020/5/3 21:22 .<br>
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/data")
public class MockDataController {

    @Autowired
    private MockDataService mockDataService;

    @GetMapping
    public SimpleResult<List<MockData>> search(@ModelAttribute MockDataQueryVo queryVo) {
        Page<MockData> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query();
        if (queryVo.getRequestId() != null) {
            queryWrapper.eq("request_id", queryVo.getRequestId());
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.page(page, queryWrapper));
    }

    @GetMapping("/{id}")
    public SimpleResult<MockData> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.removeById(id));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockData group) {
        return SimpleResultUtils.createSimpleResult(mockDataService.saveOrUpdate(group));
    }
}
