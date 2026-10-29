package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.CountData;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.service.mock.MockProjectService;
import com.fugary.simple.mock.service.mock.MockRequestService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.XmlUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.web.vo.query.MockHistoryVo;
import com.fugary.simple.mock.web.vo.query.MockJwtParamVo;
import com.fugary.simple.mock.web.vo.query.SimpleQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fugary.simple.mock.contants.MockConstants.DB_MODIFY_FROM_KEY;

/**
 * Created on 2020/5/3 21:22 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@RestController
@RequestMapping("/admin/data")
public class MockDataController {

    private static final int RESPONSE_PREVIEW_LENGTH = 61;

    private static final Set<String> PATCH_PROTECTED_FIELDS = Set.of(
            MockData.Fields.groupId, MockData.Fields.requestId, MockData.Fields.modifyFrom,
            MockConstants.CREATOR_KEY, MockConstants.CREATE_DATE_KEY,
            MockConstants.MODIFIER_KEY, MockConstants.MODIFY_DATE_KEY);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private MockRequestService mockRequestService;

    @Autowired
    private MockProjectService mockProjectService;

    @GetMapping
    public SimpleResult<List<MockData>> search(@ModelAttribute MockDataQueryVo queryVo) {
        Page<MockData> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus())
                .isNull(DB_MODIFY_FROM_KEY);
        queryWrapper.eq(queryVo.getRequestId() != null, "request_id", queryVo.getRequestId());
        if (StringUtils.isNotBlank(queryVo.getStatusCode())) {
            int statusCode = NumberUtils.toInt(queryVo.getStatusCode().substring(0, 1)) * 100;
            queryWrapper.between("status_code", statusCode, statusCode + 99);
        }
        queryWrapper.orderByAsc("status_code", "create_date");
        if (queryVo.isSummary()) {
            // 在数据库端截取预览，避免将大响应体加载到列表中。
            selectMetadata(queryWrapper, true);
        }
        Page<MockData> pageResult = mockDataService.page(page, queryWrapper);
        Map<Integer, Long> historyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> dataIds = pageResult.getRecords().stream().map(MockData::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockData> countQuery = Wrappers.<MockData>query()
                    .select("modify_from as group_key", "count(0) as data_count")
                    .in(DB_MODIFY_FROM_KEY, dataIds).groupBy(DB_MODIFY_FROM_KEY);
            historyMap = mockDataService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        long dataCount = mockDataService.count(Wrappers.<MockData>query().isNull(DB_MODIFY_FROM_KEY)
                .eq("request_id", queryVo.getRequestId()));
        return SimpleResultUtils.createSimpleResult(pageResult)
                .addInfo("dataCount", dataCount)
                .addInfo("historyMap", historyMap);
    }

    @PostMapping("/histories/{id}")
    public SimpleResult<List<MockData>> histories(@RequestBody MockDataQueryVo queryVo, @PathVariable Integer id) {
        QueryWrapper<MockData> currentQuery = Wrappers.<MockData>query().eq("id", id);
        Page<MockData> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query()
                .eq(DB_MODIFY_FROM_KEY, id);
        if (queryVo.isSummary()) {
            selectMetadata(currentQuery, true);
            selectMetadata(queryWrapper, true);
        }
        MockData currentData = mockDataService.getOne(currentQuery);
        queryWrapper.orderByDesc("data_version");
        return SimpleResultUtils.createSimpleResult(mockDataService.page(page, queryWrapper))
                .addInfo("current", currentData);
    }

    @PostMapping("/loadHistoryDiff")
    public SimpleResult<Map<String, MockData>> loadHistoryDiff(@RequestBody MockHistoryVo queryVo) {
        Integer id = queryVo.getId();
        Integer maxVersion = queryVo.getVersion();
        MockData modified = mockDataService.getById(id);
        Page<MockData> page = new Page<>(1, 2);
        mockDataService.page(page, Wrappers.<MockData>query().eq(DB_MODIFY_FROM_KEY, ObjectUtils.defaultIfNull(modified.getModifyFrom(), modified.getId()))
                .le(maxVersion != null, "data_version", maxVersion)
                .orderByDesc("data_version"));
        if (page.getRecords().isEmpty()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        } else {
            Map<String, MockData> map = new HashMap<>(2);
            List<MockData> dataList = page.getRecords();
            map.put("modified", modified);
            dataList.stream().filter(data -> !data.getId().equals(modified.getId())).findFirst().ifPresent(data -> {
                map.put("original", data);
            });
            return SimpleResultUtils.createSimpleResult(map);
        }
    }

    @PostMapping("/recoverFromHistory")
    public SimpleResult<MockData> recoverFromHistory(@RequestBody MockHistoryVo historyVo) {
        MockData history = mockDataService.getById(historyVo.getId());
        MockData target = null;
        if (history != null && history.getModifyFrom() != null) {
            target = mockDataService.getById(history.getModifyFrom());
        }
        if (history == null || target == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!mockProjectService.hasDataAuthority(target, MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        SimpleMockUtils.copyFromHistory(history, target);
        return mockDataService.newSaveOrUpdate(target); // 更新
    }

    @GetMapping("/{id}")
    public SimpleResult<MockData> get(@PathVariable("id") Integer id,
                                    @RequestParam(defaultValue = "true") boolean includeResponseBody) {
        if (includeResponseBody) {
            return SimpleResultUtils.createSimpleResult(mockDataService.getById(id));
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.getOne(selectMetadata(Wrappers.<MockData>query(), false)
                .eq("id", id)));
    }

    private QueryWrapper<MockData> selectMetadata(QueryWrapper<MockData> query, boolean preview) {
        query.select(MockData.class, field -> !MockData.Fields.responseBody.equals(field.getProperty()));
        if (preview) {
            String bodyColumn = TableInfoHelper.getTableInfo(MockData.class).getFieldList().stream()
                    .filter(field -> MockData.Fields.responseBody.equals(field.getProperty()))
                    .findFirst().orElseThrow().getColumn();
            // SQL 标准语法，MySQL、H2、PostgreSQL 均支持字符截取。
            query.select(query.getSqlSelect() + ", SUBSTRING(" + bodyColumn + " FROM 1 FOR "
                    + RESPONSE_PREVIEW_LENGTH + ") AS " + MockData.Fields.responseBodyPreview);
        }
        return query;
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        if (!mockProjectService.hasDataAuthority(id, MockConstants.AUTHORITY_DELETABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.deleteMockData(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            for (Integer id : ids) {
                if (!mockProjectService.hasDataAuthority(id, MockConstants.AUTHORITY_DELETABLE)) {
                    return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
                }
            }
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.deleteMockDatas(ids));
    }

    @PostMapping
    public SimpleResult<MockData> save(@RequestBody MockData data) {
        if (data.getId() != null) {
            MockData existData = mockDataService.getById(data.getId());
            if (existData != null
                    && !mockProjectService.hasDataAuthority(existData, MockConstants.AUTHORITY_WRITABLE)) {
                return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
            }
        } else if (!mockProjectService.hasRequestAuthority(data.getRequestId(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return mockDataService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(data));
    }

    /** 局部更新：缺失字段保持原值，显式 null 仍表示清空。 */
    @PatchMapping("/{id}")
    public SimpleResult<MockData> patch(@PathVariable Integer id, @RequestBody Map<String, Object> changes)
            throws JsonMappingException {
        MockData data = mockDataService.getById(id);
        if (data == null) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404);
        }
        if (!mockProjectService.hasDataAuthority(data, MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        // 映射字段列表不含主键及非持久化字段，再排除归属与审计字段。
        Set<String> editableFields = TableInfoHelper.getTableInfo(MockData.class).getFieldList().stream()
                .map(field -> field.getProperty())
                .filter(property -> !PATCH_PROTECTED_FIELDS.contains(property))
                .collect(Collectors.toSet());
        Map<String, Object> updates = new HashMap<>(changes);
        updates.keySet().retainAll(editableFields); // 保留显式 null，缺失字段不参与合并。
        Integer version = data.getVersion();
        objectMapper.updateValue(data, updates);
        if (!Objects.equals(version, data.getVersion())) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_1);
        }
        SimpleResult<MockData> result = mockDataService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(data));
        if (result.getResultData() != null) {
            result.getResultData().setResponseBody(null);
        }
        return result;
    }

    @PostMapping("/copyMockData/{dataId}")
    public SimpleResult copyMockData(@PathVariable("dataId") Integer id) {
        if (!mockProjectService.hasDataAuthority(id, MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.copyMockData(id, null));
    }

    @PostMapping("/markDefault")
    public SimpleResult markDefault(@RequestBody MockData data) {
        if (!mockProjectService.hasDataAuthority(data.getId(), MockConstants.AUTHORITY_WRITABLE)) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403);
        }
        return SimpleResultUtils.createSimpleResult(mockDataService.markMockDataDefault(data));
    }

    /**
     * 生成mock jwt token
     * @param jwtParam
     * @return
     */
    @PostMapping("/generateJwt")
    public SimpleResult<String> generateJwt(@RequestBody MockJwtParamVo jwtParam) {
        Algorithm algorithm = getAlgorithm(jwtParam.getAlgorithm(), jwtParam.getSecret());
        JWTCreator.Builder builder = JWT.create()
                .withPayload(jwtParam.getPayload());
        if (StringUtils.isNotBlank(jwtParam.getIssuer())) {
            builder.withIssuer(jwtParam.getIssuer());
        }
        if (jwtParam.getExpireTime() != null) {
            builder.withExpiresAt(jwtParam.getExpireTime());
        }
        String token = builder.sign(algorithm);
        return SimpleResultUtils.createSimpleResult(token);
    }

    @PostMapping("/xml2Json")
    public SimpleResult<String> xml2Json(@RequestBody SimpleQueryVo queryVo) {
        return XmlUtils.xml2Json(queryVo.getKeyword());
    }

    protected Algorithm getAlgorithm(String algorithm, String secret) {
        Map<String, Function<String, Algorithm>> config = new HashMap<>();
        config.put("HS256", Algorithm::HMAC256);
        config.put("HS384", Algorithm::HMAC384);
        config.put("HS512", Algorithm::HMAC512);
        Function<String, Algorithm> func = config.get(algorithm);
        if (func != null) {
            return func.apply(secret);
        }
        return Algorithm.HMAC256(secret);
    }
}
