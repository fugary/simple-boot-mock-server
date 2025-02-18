package com.fugary.simple.mock.web.controllers.admin;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.MockData;
import com.fugary.simple.mock.script.ScriptEngineProvider;
import com.fugary.simple.mock.service.mock.MockDataService;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.query.MockDataQueryVo;
import com.fugary.simple.mock.web.vo.query.MockJwtParamVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
        queryWrapper.orderByAsc("status_code", "create_date");
        return SimpleResultUtils.createSimpleResult(mockDataService.page(page, queryWrapper));
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
