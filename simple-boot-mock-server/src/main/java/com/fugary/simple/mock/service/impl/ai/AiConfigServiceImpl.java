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

/** Validates shared configurations and restricts management to administrators. */
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
    public AiConfig getConfig(Integer id) {
        user();
        AiConfig value = baseMapper.selectOne(Wrappers.<AiConfig>query().eq("id", id).isNull("modify_from"));
        if (value == null) {
            throw new AiException(404, "simple.error.code.404");
        }
        return value;
    }

    @Override
    public Page<AiConfig> search(AiConfigQueryVo query) {
        user();
        return baseMapper.selectPage(SimpleResultUtils.toPage(query), Wrappers.<AiConfig>query()
            .isNull("modify_from").select(AiConfig.class, field -> !"apiKey".equals(field.getProperty()))
            .like(StringUtils.isNotBlank(query.getKeyword()), "config_name", query.getKeyword())
            .eq(StringUtils.isNotBlank(query.getProvider()), "provider", StringUtils.trim(query.getProvider()))
            .like(StringUtils.isNotBlank(query.getDefaultModel()), "default_model", StringUtils.trim(query.getDefaultModel()))
            .eq(query.getStatus() != null, "status", query.getStatus()).orderByDesc("id"));
    }

    @Override
    @Transactional
    public AiConfig saveConfig(AiConfig input) {
        return saveConfig(input, true);
    }

    private AiConfig saveConfig(AiConfig input, boolean preserveBlankKey) {
        requireAdmin();
        String owner = user();
        AiConfig old = input.getId() == null ? null : getConfig(input.getId());
        AiConfig value = new AiConfig();
        value.setId(input.getId());
        value.setConfigName(StringUtils.trim(input.getConfigName()));
        normalizeConnection(input, value);
        value.setDefaultModel(StringUtils.trim(input.getDefaultModel()));
        value.setStatus(input.getStatus());
        value.setIsDefault(input.getIsDefault());
        validateConfig(value);
        if (preserveBlankKey && StringUtils.isBlank(value.getApiKey()) && old != null) {
            requireSameConnection(value, old);
            value.setApiKey(old.getApiKey());
        }
        if (StringUtils.length(value.getApiKey()) > 4096) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        value.setUserName(old == null ? owner : old.getUserName());
        value.setStatus(Integer.valueOf(0).equals(value.getStatus()) ? 0 : 1);
        value.setIsDefault(Integer.valueOf(1).equals(value.getIsDefault()) ? 1 : 0);
        value.setCreator(old == null ? owner : old.getCreator());
        value.setCreateDate(old == null ? new Date() : old.getCreateDate());
        SimpleMockUtils.addAuditInfo(value);
        if (value.getIsDefault() == 1) {
            for (AiConfig previous : list(Wrappers.<AiConfig>query().isNull("modify_from")
                    .eq("is_default", 1).ne(value.getId() != null, "id", value.getId()))) {
                archive(previous);
                previous.setIsDefault(0);
                SimpleMockUtils.addAuditInfo(previous);
                updateVersion(previous);
            }
        }
        value.setVersion(old == null ? 1 : old.getVersion() + 1);
        if (old == null) {
            baseMapper.insert(value);
        } else {
            archive(old);
            updateVersion(value);
        }
        return value;
    }

    @Override
    @Transactional
    public boolean deleteConfig(Integer id) {
        requireAdmin();
        getConfig(id);
        baseMapper.delete(Wrappers.<AiConfig>query().eq("modify_from", id));
        return baseMapper.deleteById(id) > 0;
    }

    private void updateVersion(AiConfig value) {
        if (baseMapper.update(value, Wrappers.<AiConfig>update().eq("id", value.getId())
                .eq("version", value.getVersion() - 1).isNull("modify_from")) != 1) {
            throw new AiException(409, "simple.ai.invalidConfig");
        }
    }

    private void archive(AiConfig current) {
        AiConfig snapshot = new AiConfig();
        BeanUtils.copyProperties(current, snapshot);
        snapshot.setId(null);
        snapshot.setModifyFrom(current.getId());
        baseMapper.insert(snapshot);
        current.setVersion(current.getVersion() + 1);
    }

    @Override
    public Page<AiConfig> histories(Integer id, AiConfigQueryVo query) {
        requireAdmin();
        getConfig(id);
        return baseMapper.selectPage(SimpleResultUtils.toPage(query), Wrappers.<AiConfig>query()
                .eq("modify_from", id).orderByDesc("version"));
    }

    private AiConfig historyRecord(Integer id) {
        requireAdmin();
        AiConfig record = getById(id);
        if (record == null) {
            throw new AiException(404, "simple.error.code.404");
        }
        getConfig(record.getModifyFrom() == null ? id : record.getModifyFrom());
        return record;
    }

    @Override
    public Map<String, AiConfig> historyDiff(Integer id) {
        AiConfig modified = historyRecord(id);
        List<AiConfig> previous = baseMapper.selectPage(new Page<AiConfig>(1, 1),
                Wrappers.<AiConfig>query()
                        .eq("modify_from", modified.getModifyFrom() == null ? id : modified.getModifyFrom())
                        .lt("version", modified.getVersion()).orderByDesc("version")).getRecords();
        Map<String, AiConfig> result = new LinkedHashMap<>();
        result.put("modified", modified);
        result.put("original", previous.isEmpty() ? new AiConfig() : previous.get(0));
        return result;
    }

    @Override
    @Transactional
    public AiConfig recover(Integer id) {
        AiConfig snapshot = historyRecord(id);
        if (snapshot.getModifyFrom() == null) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        snapshot.setId(snapshot.getModifyFrom());
        return saveConfig(snapshot, false);
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
        requireAdmin();
        if (input == null) {
            throw new AiException(400, "simple.ai.invalidConfig");
        }
        AiConfig stored = input.getId() == null ? null : getConfig(input.getId());
        AiConfig config = new AiConfig();
        normalizeConnection(input, config);
        validateConnection(config);
        if (stored != null && StringUtils.isBlank(config.getApiKey())) {
            requireSameConnection(config, stored);
            config.setApiKey(stored.getApiKey());
        }
        return config;
    }

    private void normalizeConnection(AiConfig input, AiConfig target) {
        target.setProvider(AiProvider.fromCode(StringUtils.trim(input.getProvider())).getCode());
        target.setBaseUrl(normalizeBaseUrl(input.getBaseUrl()));
        target.setApiKey(StringUtils.trim(input.getApiKey()));
    }

    private String normalizeBaseUrl(String baseUrl) {
        return StringUtils.stripEnd(StringUtils.trim(baseUrl), "/");
    }

    @Override
    public AiConfig getDefaultConfig() {
        user();
        return getOne(Wrappers.<AiConfig>query()
            .isNull("modify_from").eq("is_default", 1).eq("status", 1));
    }

    @Override
    public List<AiConfigOption> options() {
        String owner = user();
        Map<Integer, AiConfigOption> options = new LinkedHashMap<>();
        for (AiConfig config : list(Wrappers.<AiConfig>query()
            .isNull("modify_from").select("id", "config_name", "provider", "base_url", "is_default", "status")
            .orderByDesc("id"))) {
            AiConfigOption option = new AiConfigOption();
            BeanUtils.copyProperties(config, option);
            options.put(config.getId(), option);
        }
        for (AiLog history : logMapper.selectList(Wrappers.<AiLog>query()
            .select("config_id", "config_name").eq(!SecurityUtils.isAdminUser(), "user_name", owner).isNotNull("config_id")
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

    @Override
    public AiConfig getUsableConfig(Integer id) {
        AiConfig config = getConfig(id);
        if (!config.isEnabled()) {
            throw new AiException(403, "simple.error.code.403");
        }
        return config;
    }

    private void requireAdmin() {
        user();
        if (!SecurityUtils.isAdminUser()) {
            throw new AiException(403, "simple.error.code.403");
        }
    }

    private void requireSameConnection(AiConfig input, AiConfig stored) {
        if (!input.getProvider().equalsIgnoreCase(stored.getProvider())
            || !normalizeBaseUrl(input.getBaseUrl()).equals(normalizeBaseUrl(stored.getBaseUrl()))) {
            throw new AiException(400, "simple.ai.keyRequired");
        }
    }

}
