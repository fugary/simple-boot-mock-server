package com.fugary.simple.mock.entity.mock;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Map;

/**
 * Create date 2025/4/28<br>
 *
 * @author gary.fu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CountData extends GroupByData<Long> {

    public CountData(Map<String, Object> countMap) {
        super(countMap, Long.class);
    }

    @Override
    protected Object getGroupValueObj(Map<String, Object> countMap) {
        return ObjectUtils.defaultIfNull(countMap.get("DATA_COUNT"), countMap.get("data_count"));
    }

    public Long getDataCount() {
        return getGroupValue();
    }
}
