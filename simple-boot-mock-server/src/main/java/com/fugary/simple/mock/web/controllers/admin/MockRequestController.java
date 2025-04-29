package com.fugary.simple.mock.web.controllers.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.CountData;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.entity.mock.MockRequest;
import com.fugary.simple.mock.entity.mock.MockSchema;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.service.mock.MockSchemaService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockRequestQueryVo;
import com.fugary.simple.mock.web.vo.query.MockSchemaQueryVo;
import com.fugary.simple.mock.web.vo.result.MockSchemaResultVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Autowired
    private MockSchemaService mockSchemaService;

    @Autowired
    private MockDataService mockDataService;

    @GetMapping
    public SimpleResult<List<MockRequest>> search(@ModelAttribute MockRequestQueryVo queryVo) {
        Page<MockRequest> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockRequest> queryWrapper = Wrappers.<MockRequest>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus());
        if (queryVo.getGroupId() != null) {
            queryWrapper.eq("group_id", queryVo.getGroupId());
        }
        String keyword = StringUtils.trimToEmpty(queryVo.getKeyword());
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("request_path", keyword)
                    .or().like("request_name", keyword)
                    .or().like("description", keyword));
        }
        if (StringUtils.isNotBlank(queryVo.getMethod())) {
            queryWrapper.and(wrapper -> wrapper.eq("method", queryVo.getMethod()));
        }
        queryWrapper.orderByAsc("request_path", "method");
        Page<MockRequest> pageResult = mockRequestService.page(page, queryWrapper);
        Map<Integer, Long> countMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> requestIds = pageResult.getRecords().stream().map(MockRequest::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockData> countQuery = Wrappers.<MockData>query()
                    .select("request_id as group_key", "count(0) as data_count")
                    .in("request_id", requestIds).groupBy("request_id");
            countMap = mockDataService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        return SimpleResultUtils.createSimpleResult(pageResult).addInfo("countMap", countMap);
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
        return SimpleResultUtils.createSimpleResult(mockRequestService.copyMockRequest(id, null));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.deleteMockRequest(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockRequestService.deleteMockRequests(ids));
    }

    @PostMapping
    public SimpleResult<MockRequest> save(@RequestBody MockRequest request) {
        mockRequestService.saveOrUpdate(SimpleMockUtils.addAuditInfo(request));
        return SimpleResultUtils.createSimpleResult(request);
    }

    /**
     * 获取Schema列表
     * @param queryVo
     * @return
     */
    @GetMapping("/loadSchemas")
    public SimpleResult<MockSchemaResultVo> loadSchemas(@ModelAttribute MockSchemaQueryVo queryVo) {
        MockRequest request = mockRequestService.getById(queryVo.getRequestId());
        if (request == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        List<MockSchema> schemas = mockSchemaService.querySchemas(queryVo.getRequestId(), queryVo.getDataId());
        MockSchemaResultVo resultVo = new MockSchemaResultVo();
        resultVo.setSchemas(schemas);
        List<MockSchema> groupSchemas = mockSchemaService.queryGroupSchemas(request.getGroupId());
        for (MockSchema groupSchema : groupSchemas) {
            if (MockConstants.MOCK_SCHEMA_BODY_TYPE_COMPONENT.equals(groupSchema.getBodyType())) {
                resultVo.setComponentSchema(groupSchema);
            }
        }
        return SimpleResultUtils.createSimpleResult(resultVo);
    }
}
