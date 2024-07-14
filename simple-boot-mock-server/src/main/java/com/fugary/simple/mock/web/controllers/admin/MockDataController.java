package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.service.mock.MockDataService;
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

    @Autowired
    private ScriptEngineProvider scriptEngineProvider;

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

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockDataService.removeByIds(ids));
    }

    @PostMapping
    public SimpleResult save(@RequestBody MockData data) {
        return SimpleResultUtils.createSimpleResult(mockDataService.saveOrUpdate(SimpleMockUtils.addAuditInfo(data)));
    }

    @PostMapping("/preview")
    public String previewResponse(@RequestBody String data) {
        return scriptEngineProvider.mock(data);
    }

    @PostMapping("/markDefault")
    public SimpleResult markDefault(@RequestBody MockData data) {
        return SimpleResultUtils.createSimpleResult(mockDataService.markMockDataDefault(data));
    }
}
