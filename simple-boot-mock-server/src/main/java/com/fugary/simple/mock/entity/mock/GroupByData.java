package com.fugary.simple.mock.entity.mock;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Create date 2025/12/24<br>
 *
 * @author gary.fu
 */
@Data
public class GroupByData<T> implements Serializable {

    private static final long serialVersionUID = 2377760049746716196L;
    private String groupKey;
    private T groupValue;

    public GroupByData() {
    }

    public GroupByData(Map<String, Object> countMap, Class<T> clazz) {
        Object groupKeyObj = getGroupKeyObj(countMap);
        this.groupKey = Objects.toString(groupKeyObj, null);
        Object dataCountObj = getGroupValueObj(countMap);
        if (clazz.isInstance(dataCountObj)) {
            this.groupValue = clazz.cast(dataCountObj);
        } else if (Date.class.isAssignableFrom(clazz) && dataCountObj instanceof LocalDateTime) {
            LocalDateTime value = (LocalDateTime) dataCountObj;
            this.groupValue = (T) Date.from(value.toInstant(ZoneOffset.UTC));
        }
    }

    protected Object getGroupKeyObj(Map<String, Object> countMap){
        return ObjectUtils.defaultIfNull(countMap.get("GROUP_KEY"), countMap.get("group_key"));
    }

    protected Object getGroupValueObj(Map<String, Object> countMap){
        return ObjectUtils.defaultIfNull(countMap.get("GROUP_VALUE"), countMap.get("group_value"));
    }
}
