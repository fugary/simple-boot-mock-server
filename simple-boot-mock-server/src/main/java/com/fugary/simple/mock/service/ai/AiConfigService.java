package com.fugary.simple.mock.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.AiConfig;
import com.fugary.simple.mock.web.vo.query.AiConfigQueryVo;
import com.fugary.simple.mock.web.vo.AiConfigOption;
import java.util.List;

public interface AiConfigService extends IService<AiConfig> {
    List<AiConfigOption> options();
    AiConfig resolveConnection(AiConfig config);
    AiConfig getOwned(Integer id);
    AiConfig saveConfig(AiConfig config);
    boolean deleteOwned(Integer id);
    Page<AiConfig> search(AiConfigQueryVo query);
}
