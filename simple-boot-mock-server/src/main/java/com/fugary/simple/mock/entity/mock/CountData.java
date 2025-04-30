package com.fugary.simple.mock.entity.mock;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Create date 2025/4/28<br>
 *
 * @author gary.fu
 */
@Data
public class CountData implements Serializable {

    private static final long serialVersionUID = 2377760049746716196L;
    private String groupKey;
    private Long dataCount;

    public CountData() {
    }

    public CountData(Map<String, Object> countMap) {
        Object groupKeyObj = ObjectUtils.defaultIfNull(countMap.get("GROUP_KEY"), countMap.get("group_key"));
        this.groupKey = Objects.toString(groupKeyObj);
        Object dataCountObj = ObjectUtils.defaultIfNull(countMap.get("DATA_COUNT"), countMap.get("data_count"));
        if (dataCountObj instanceof Long) {
            this.dataCount = (Long) dataCountObj;
        }
    }
}
