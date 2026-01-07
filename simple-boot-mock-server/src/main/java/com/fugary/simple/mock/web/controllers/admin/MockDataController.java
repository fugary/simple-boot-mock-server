package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.CountData;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.utils.*;
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

    @Autowired
    private MockDataService mockDataService;

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
        return SimpleResultUtils.createSimpleResult(pageResult).addInfo("historyMap", historyMap);
    }

    @PostMapping("/histories/{id}")
    public SimpleResult<List<MockData>> histories(@RequestBody MockDataQueryVo queryVo, @PathVariable Integer id) {
        MockData currentData = mockDataService.getById(id);
        Page<MockData> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query()
                .eq(DB_MODIFY_FROM_KEY, id);
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
        SimpleMockUtils.copyFromHistory(history, target);
        return mockDataService.newSaveOrUpdate(target); // 更新
    }

    @GetMapping("/{id}")
    public SimpleResult<MockData> get(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.getById(id));
    }

    @DeleteMapping("/{id}")
    public SimpleResult remove(@PathVariable("id") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.deleteMockData(id));
    }

    @DeleteMapping("/removeByIds/{ids}")
    public SimpleResult removeByIds(@PathVariable("ids") List<Integer> ids) {
        return SimpleResultUtils.createSimpleResult(mockDataService.deleteMockDatas(ids));
    }

    @PostMapping
    public SimpleResult<MockData> save(@RequestBody MockData data) {
        return mockDataService.newSaveOrUpdate(SimpleMockUtils.addAuditInfo(data));
    }

    @PostMapping("/copyMockData/{dataId}")
    public SimpleResult copyMockData(@PathVariable("dataId") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.copyMockData(id, null));
    }

    @PostMapping("/markDefault")
    public SimpleResult markDefault(@RequestBody MockData data) {
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
        String resultStr =  "";
        if (StringUtils.isNotBlank(queryVo.getKeyword()) && MockJsUtils.isXml(queryVo.getKeyword())) {
            JsonNode jsonNode;
            try {
                jsonNode = XmlUtils.getMapper().readTree(queryVo.getKeyword());
            } catch (JsonProcessingException e) {
                log.error("XML解析失败", e);
                return SimpleResultUtils.createError(e.getOriginalMessage());
            }
            resultStr = JsonUtils.toJson(jsonNode);
        }
        return SimpleResultUtils.createSimpleResult(resultStr);
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
