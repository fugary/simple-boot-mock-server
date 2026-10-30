package com.fugary.simple.mock.service.ai;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fugary.simple.mock.entity.mock.AiLog;
import com.fugary.simple.mock.web.vo.query.AiLogQueryVo;

public interface AiLogService extends IService<AiLog> {
    Page<AiLog> search(AiLogQueryVo query);
    AiLog getOwned(Integer id);
    boolean deleteOwned(Integer id);
    void record(AiLog log);
}
