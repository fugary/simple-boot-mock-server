package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.entity.mock.CountData;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.web.vo.query.MockHistoryVo;
import com.fugary.simple.mock.web.vo.query.MockJwtParamVo;
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
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query()
                .eq(queryVo.getStatus() != null, "status", queryVo.getStatus())
                .isNull("modify_from");
        if (queryVo.getRequestId() != null) {
            queryWrapper.eq("request_id", queryVo.getRequestId());
        }
        queryWrapper.orderByAsc("status_code", "create_date");
        Page<MockData> pageResult = mockDataService.page(page, queryWrapper);
        Map<Integer, Long> historyMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(pageResult.getRecords())) {
            List<Integer> dataIds = pageResult.getRecords().stream().map(MockData::getId)
                    .collect(Collectors.toList());
            QueryWrapper<MockData> countQuery = Wrappers.<MockData>query()
                    .select("modify_from as group_key", "count(0) as data_count")
                    .in("modify_from", dataIds).groupBy("modify_from");
            historyMap = mockDataService.listMaps(countQuery).stream().map(CountData::new)
                    .collect(Collectors.toMap(data -> NumberUtils.toInt(data.getGroupKey()),
                            CountData::getDataCount));
        }
        return SimpleResultUtils.createSimpleResult(pageResult).addInfo("historyMap", historyMap);
    }

    @PostMapping("/histories/{id}")
    public SimpleResult<List<MockData>> histories(@RequestBody MockDataQueryVo queryVo, @PathVariable Integer id) {
        Page<MockData> page = SimpleResultUtils.toPage(queryVo);
        QueryWrapper<MockData> queryWrapper = Wrappers.<MockData>query()
                .eq("modify_from", id);
        queryWrapper.orderByDesc("data_version");
        return SimpleResultUtils.createSimpleResult(mockDataService.page(page, queryWrapper));
    }

    @PostMapping("/loadHistoryDiff")
    public SimpleResult<Map<String, MockData>> loadHistoryDiff(@RequestBody MockHistoryVo queryVo) {
        Integer id = queryVo.getId();
        Integer maxVersion = queryVo.getVersion();
        MockData modified = mockDataService.getById(id);
        Page<MockData> page = new Page<>(1, 2);
        mockDataService.page(page, Wrappers.<MockData>query().eq("modify_from", ObjectUtils.defaultIfNull(modified.getModifyFrom(), modified.getId()))
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
        mockDataService.saveOrUpdate(SimpleMockUtils.addAuditInfo(data));
        return SimpleResultUtils.createSimpleResult(data);
    }

    @PostMapping("/copyMockData/{dataId}")
    public SimpleResult copyMockData(@PathVariable("dataId") Integer id) {
        return SimpleResultUtils.createSimpleResult(mockDataService.copyMockData(id));
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
