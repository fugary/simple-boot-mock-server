package com.fugary.simple.mock.service.impl.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.mapper.mock.AiConfigMapper;
import com.fugary.simple.mock.service.ai.AiConfigService;
import com.fugary.simple.mock.service.ai.AiProvider;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.query.AiConfigQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fugary.simple.mock.web.vo.AiConfigOption;
import com.fugary.simple.mock.mapper.mock.AiLogMapper;
import com.fugary.simple.mock.entity.mock.AiLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import java.util.Date;

/** Owns configuration validation, user isolation and atomic default selection. */
@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {
    @Autowired
    private AiLogMapper logMapper;

    private String user() {
        String name = SecurityUtils.getLoginUserName();
        if (StringUtils.isBlank(name)) {
            throw new AiException(401, "simple.error.code.401");
        }
        return name;
    }

    @Override
    public AiConfig getOwned(Integer id) {
        AiConfig value = baseMapper.selectOne(Wrappers.<AiConfig>query().eq("id", id).eq("user_name", user()));
        if (value == null) {
            throw new AiException(404, "simple.error.code.404");
        }
        return value;
    }

    @Override
    public Page<AiConfig> search(AiConfigQueryVo query) {
        return baseMapper.selectPage(SimpleResultUtils.toPage(query), Wrappers.<AiConfig>query()
            .select(AiConfig.class, field -> !"apiKey".equals(field.getProperty()))
            .eq("user_name", user()).like(StringUtils.isNotBlank(query.getKeyword()), "config_name", query.getKeyword())
            .eq(StringUtils.isNotBlank(query.getProvider()), "provider", StringUtils.trim(query.getProvider()))
            .like(StringUtils.isNotBlank(query.getDefaultModel()), "default_model", StringUtils.trim(query.getDefaultModel()))
            .eq(query.getStatus() != null, "status", query.getStatus()).orderByDesc("id"));
    }

    @Override
    @Transactional
    public AiConfig saveConfig(AiConfig input) {
        String owner = user();
        AiConfig old = input.getId() == null ? null : getOwned(input.getId());
        AiConfig value = new AiConfig();
        value.setId(input.getId());
        value.setConfigName(input.getConfigName());
        value.setProvider(input.getProvider());
        value.setBaseUrl(input.getBaseUrl());
        value.setDefaultModel(input.getDefaultModel());
        value.setApiKey(input.getApiKey());
        value.setStatus(input.getStatus());
        value.setIsDefault(input.getIsDefault());
        validateConfig(value);
        String provider = AiProvider.fromCode(value.getProvider()).getCode();
        if (StringUtils.isBlank(value.getApiKey()) && old != null) {
            requireSameConnection(value, old);
            value.setApiKey(old.getApiKey());
        }
        if (StringUtils.length(value.getApiKey()) > 4096) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        value.setProvider(provider);
        value.setUserName(owner);
        value.setStatus(Integer.valueOf(0).equals(value.getStatus()) ? 0 : 1);
        value.setIsDefault(Integer.valueOf(1).equals(value.getIsDefault()) ? 1 : 0);
        value.setCreator(old == null ? owner : old.getCreator());
        value.setCreateDate(old == null ? new Date() : old.getCreateDate());
        SimpleMockUtils.addAuditInfo(value);
        if (value.getIsDefault() == 1) {
            baseMapper.update(null, Wrappers.<AiConfig>update().eq("user_name", owner).set("is_default", 0));
        }
        if (old == null) {
            baseMapper.insert(value);
        } else {
            baseMapper.updateById(value);
        }
        return value;
    }

    @Override
    public boolean deleteOwned(Integer id) {
        getOwned(id);
        return baseMapper.deleteById(id) > 0;
    }

    private void validateConfig(AiConfig value) {
        if (StringUtils.isAnyBlank(value.getConfigName(), value.getBaseUrl(), value.getDefaultModel())
            || value.getConfigName().length() > 255 || value.getDefaultModel().length() > 255 || value.getBaseUrl().length() > 2048) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        validateConnection(value);
    }

    private void validateConnection(AiConfig value) {
        if (StringUtils.isBlank(value.getBaseUrl()) || value.getBaseUrl().length() > 2048 || StringUtils.length(value.getApiKey()) > 4096) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        AiProvider.fromCode(value.getProvider());
        URI uri;
        try {
            uri = URI.create(value.getBaseUrl());
        } catch (Exception e) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new AiException(400, "simple.ai.invalidUrl");
        }
    }

    @Override
    public AiConfig resolveConnection(AiConfig input) {
        user();
        if (input == null) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        AiConfig stored = input.getId() == null ? null : getOwned(input.getId());
        AiConfig config = new AiConfig();
        config.setProvider(AiProvider.fromCode(input.getProvider()).getCode());
        config.setBaseUrl(StringUtils.stripEnd(StringUtils.trim(input.getBaseUrl()), "/"));
        config.setApiKey(input.getApiKey());
        validateConnection(config);
        if (stored != null && StringUtils.isBlank(config.getApiKey())) {
            requireSameConnection(config, stored);
            config.setApiKey(stored.getApiKey());
        }
        return config;
    }

    @Override
    public AiConfig getDefaultConfig() {
        return getOne(Wrappers.<AiConfig>query().eq("user_name", user())
            .eq("is_default", 1).eq("status", 1));
    }

    @Override
    public List<AiConfigOption> options() {
        String owner = user();
        Map<Integer, AiConfigOption> options = new LinkedHashMap<>();
        for (AiConfig config : list(Wrappers.<AiConfig>query()
            .select("id", "config_name", "provider", "base_url", "is_default", "status")
            .eq("user_name", owner).orderByDesc("id"))) {
            AiConfigOption option = new AiConfigOption();
            BeanUtils.copyProperties(config, option);
            options.put(config.getId(), option);
        }
        for (AiLog history : logMapper.selectList(Wrappers.<AiLog>query()
            .select("config_id", "config_name").eq("user_name", owner).isNotNull("config_id")
            .groupBy("config_id", "config_name").orderByDesc("config_id"))) {
            if (!options.containsKey(history.getConfigId())) {
                AiConfigOption option = new AiConfigOption();
                option.setId(history.getConfigId());
                option.setConfigName(history.getConfigName());
                option.setDeleted(true);
                options.put(option.getId(), option);
            }
        }
        return new ArrayList<>(options.values());
    }

    private void requireSameConnection(AiConfig input, AiConfig stored) {
        if (!input.getProvider().equalsIgnoreCase(stored.getProvider())
            || !StringUtils.stripEnd(input.getBaseUrl(), "/").equals(StringUtils.stripEnd(stored.getBaseUrl(), "/"))) {
            throw new AiException(400, "simple.ai.keyRequired");
        }
    }

}
