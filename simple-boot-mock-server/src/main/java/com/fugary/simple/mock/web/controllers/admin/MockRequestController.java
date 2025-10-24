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
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.web.vo.query.MockHistoryVo;
import com.fugary.simple.mock.web.vo.query.MockRequestQueryVo;
import com.fugary.simple.mock.web.vo.query.MockSchemaQueryVo;
import com.fugary.simple.mock.web.vo.result.MockSchemaResultVo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;

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
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus())
                .isNull(DB_MODIFY_FROM_KEY);
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
        if (queryVo.getHasData() != null) {
            queryWrapper.exists(queryVo.getHasData(),
                    "select 1 from t_mock_data where t_mock_data.request_id=t_mock_request.id");
            queryWrapper.notExists(!queryVo.getHasData(),
                    "select 1 from t_mock_data where t_mock_data.request_id=t_mock_request.id");
        }
        queryWrapper.orderByAsc("request_path", "method");
        Page<MockRequest> pageResult = mockRequestService.page(page, queryWrapper);
        Map<Integer, Long> countMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> requestIds = pageResult.getRecords().stream().map(MockRequest::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockData> countQuery = Wrappers.<MockData>query()
                    .select("request_id as group_key", "count(0) as data_count")
                    .in("request_id", requestIds).isNull(DB_MODIFY_FROM_KEY).groupBy("request_id");
            countMap = mockDataService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        Map<Integer, Long> historyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> dataIds = pageResult.getRecords().stream().map(MockRequest::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockRequest> countQuery = Wrappers.<MockRequest>query()
                    .select("modify_from as group_key", "count(0) as data_count")
                    .in(DB_MODIFY_FROM_KEY, dataIds).groupBy(DB_MODIFY_FROM_KEY);
            historyMap = mockRequestService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        return SimpleResultUtils.createSimpleResult(pageResult).addInfo("countMap", countMap)
                .addInfo("historyMap", historyMap);
    }

    @PostMapping("/histories/{id}")
    public SimpleResult<List<MockRequest>> histories(@RequestBody MockDataQueryVo queryVo, @PathVariable Integer id) {
        MockRequest currentData = mockRequestService.getById(id);
        Page<MockRequest> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockRequest> queryWrapper = Wrappers.<MockRequest>query()
                .eq(DB_MODIFY_FROM_KEY, id);
        queryWrapper.orderByDesc("data_version");
        return SimpleResultUtils.createSimpleResult(mockRequestService.page(page, queryWrapper))
                .addInfo("current", currentData);
    }

    @PostMapping("/loadHistoryDiff")
    public SimpleResult<Map<String, MockRequest>> loadHistoryDiff(@RequestBody MockHistoryVo queryVo) {
        Integer id = queryVo.getId();
        Integer maxVersion = queryVo.getVersion();
        MockRequest modified = mockRequestService.getById(id);
        Page<MockRequest> page = new Page<>(1, 2);
        mockRequestService.page(page, Wrappers.<MockRequest>query().eq(DB_MODIFY_FROM_KEY, ObjectUtils.defaultIfNull(modified.getModifyFrom(), modified.getId()))
                .le(maxVersion != null, "data_version", maxVersion)
                .orderByDesc("data_version"));
        if (page.getRecords().isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        } else {
            Map<String, MockRequest> map = new HashMap<>(2);
            List<MockRequest> dataList = page.getRecords();
            map.put("modified", modified);
            dataList.stream().filter(data -> !data.getId().equals(modified.getId())).findFirst().ifPresent(data -> {
                map.put("original", data);
            });
            return SimpleResultUtils.createSimpleResult(map);
        }
    }

    @PostMapping("/recoverFromHistory")
    public SimpleResult<MockRequest> recoverFromHistory(@RequestBody MockHistoryVo historyVo) {
        MockRequest history = mockRequestService.getById(historyVo.getId());
        MockRequest target = null;
        if (history != null && history.getModifyFrom() != null) {
            target = mockRequestService.getById(history.getModifyFrom());
        }
        if (history == null || target == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        SimpleMockUtils.copyFromHistory(history, target);
        return mockRequestService.newSaveOrUpdate(target); // 更新
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
        return mockRequestService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(request));
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
