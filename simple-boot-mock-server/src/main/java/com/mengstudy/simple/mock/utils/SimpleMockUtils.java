package com.mengstudy.simple.mock.utils;

import com.mengstudy.simple.mock.entity.mock.MockBase;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.UUID;

/**
 * Created on 2020/5/6 9:06 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor
public class SimpleMockUtils {
    /**
     * 生成uuid
     *
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * @param target
     * @param <T>
     */
    public static <T extends MockBase> T addAuditInfo(T target) {
        Date currentDate = new Date();
        if (target != null) {
            if (target.getId() == null) {
                target.setCreateDate(currentDate);
            } else {
                target.setModifyDate(currentDate);
            }
        }
        return target;
    }
}
