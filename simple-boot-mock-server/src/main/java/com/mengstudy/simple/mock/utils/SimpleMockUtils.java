package com.mengstudy.simple.mock.utils;

import com.mengstudy.simple.mock.contants.MockConstants;
import com.mengstudy.simple.mock.entity.mock.MockBase;
import com.mengstudy.simple.mock.entity.mock.MockData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created on 2020/5/6 9:06 .<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    /**
     * 判断是否是默认响应数据
     *
     * @param mockData
     * @return
     */
    public static boolean isDefault(MockData mockData) {
        return getDefaultFlag(mockData.getDefaultFlag()) == 1;
    }

    /**
     * 计算defaultFlag
     *
     * @param defaultFlag
     * @return
     */
    public static int getDefaultFlag(Integer defaultFlag) {
        return defaultFlag == null || defaultFlag == 0 ? 0 : 1;
    }

    /**
     * mock 预览
     *
     * @param request
     * @return
     */
    public static boolean isMockPreview(HttpServletRequest request) {
        return BooleanUtils.toBoolean(StringUtils
                .defaultIfBlank(request.getHeader(MockConstants.MOCK_DATA_PREVIEW_HEADER), Boolean.FALSE.toString()));
    }

    /**
     * 计算保存为json的头信息
     *
     * @param headers
     * @return
     */
    public static HttpHeaders calcHeaders(String headers) {
        if (StringUtils.isNotBlank(headers)) {
            List<Map<String, String>> headerList = JsonUtils.fromJson(headers, List.class);
            HttpHeaders httpHeaders = new HttpHeaders();
            headerList.stream().forEach(map -> httpHeaders.add(map.get("name"), map.get("value")));
            return httpHeaders;
        }
        return null;
    }

    /**
     * 是否是可用proxyUrl
     * @param proxyUrl
     * @return
     */
    public static boolean isValidProxyUrl(String proxyUrl) {
        return StringUtils.isNotBlank(proxyUrl) && proxyUrl.matches("https?://.*");
    }
}
