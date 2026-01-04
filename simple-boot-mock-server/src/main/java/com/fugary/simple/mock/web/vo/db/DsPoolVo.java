package com.fugary.simple.mock.web.vo.db;

import com.fugary.simple.mock.utils.JsonUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DsPoolVo implements Serializable {

    private int maxPoolSize;

    private int totalCount;

    private int activeCount;

    private int idleCount;

    private int waitingCount;

    private String info;

    private String type;

    public DsPoolVo(String type, String info) {
        this.type = type;
        this.info = info;
    }

    public DsPoolVo(String type, Map<String, String> info) {
        this.type = type;
        this.info = JsonUtils.toJson(info);
    }
}
