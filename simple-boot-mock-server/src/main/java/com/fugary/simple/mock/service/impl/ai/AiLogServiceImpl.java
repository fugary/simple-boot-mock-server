package com.fugary.simple.mock.service.impl.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.mapper.mock.AiLogMapper;
import com.fugary.simple.mock.service.ai.AiLogService;
import com.fugary.simple.mock.service.ai.AiException;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.SimpleMockUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** Keeps execution history independent of configuration deletion and scoped to its owner. */
@Service
public class AiLogServiceImpl extends ServiceImpl<AiLogMapper, AiLog> implements AiLogService {
    private String user() {
        String owner = SecurityUtils.getLoginUserName();
        if (StringUtils.isBlank(owner)) {
            throw new AiException(401, "simple.error.code.401");
        }
        return owner;
    }

    @Override
    public Page<AiLog> search(AiLogQueryVo query) {
        validateQuery(query);
        String keyword = StringUtils.trimToEmpty(query.getKeyword());
        return page(SimpleResultUtils.toPage(query), Wrappers.<AiLog>query()
            .select("id", "user_name", "config_id", "config_name", "model", "status", "duration_ms",
                "prompt_tokens", "completion_tokens", "total_tokens", "create_date", "error_message")
            .eq("user_name", user()).eq(query.getConfigId() != null, "config_id", query.getConfigId())
            .like(StringUtils.isNotBlank(query.getModel()), "model", StringUtils.trim(query.getModel()))
            .ge(query.getStartDate() != null, "create_date", query.getStartDate())
            .le(query.getEndDate() != null, "create_date", query.getEndDate())
            .and(StringUtils.isNotBlank(keyword), filter -> filter.like("config_name", keyword)
                .or().like("prompt", keyword).or().like("response_text", keyword).or().like("error_message", keyword))
            .eq(query.getStatus() != null, "status", query.getStatus()).orderByDesc("id"));
    }

    @Override
    public AiLog getOwned(Integer id) {
        AiLog value = getOne(Wrappers.<AiLog>query().eq("id", id).eq("user_name", user()));
        if (value == null) {
            throw new AiException(404, "simple.error.code.404");
        }
        return value;
    }

    @Override
    public boolean deleteOwned(Integer id) {
        getOwned(id);
        return removeById(id);
    }

    @Override
    public void record(AiLog log) {
        log.setId(null);
        log.setUserName(user());
        SimpleMockUtils.addAuditInfo(log);
        save(log);
    }

    private void validateQuery(AiLogQueryVo query) {
        if (StringUtils.length(query.getKeyword()) > 255 || StringUtils.length(query.getModel()) > 255
            || (query.getStatus() != null && query.getStatus() != 0 && query.getStatus() != 1)
            || (query.getConfigId() != null && query.getConfigId() <= 0)
            || (query.getStartDate() != null && query.getEndDate() != null && query.getStartDate().after(query.getEndDate()))
            || (query.getPage() != null && (query.getPage().getPageSize() < 1 || query.getPage().getPageSize() > 100
                || query.getPage().getPageNumber() < 1 || query.getPage().getPageNumber() > Integer.MAX_VALUE))) {
            throw new AiException(400, "simple.error.code.400");
        }
    }

}
