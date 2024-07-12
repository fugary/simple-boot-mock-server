package com.fugary.simple.mock.utils.security;

import com.fugary.simple.mock.contants.MockConstants;
import com.fugary.simple.mock.entity.mock.MockUser;
import com.fugary.simple.mock.utils.servlet.HttpRequestUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Create date 2024/7/5<br>
 *
 * @author gary.fu
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtils {

    public static final String ADMIN_USER = "admin";

    /**
     * 获取登录用户
     *
     * @return
     */
    public static MockUser getLoginUser() {
        HttpServletRequest request = HttpRequestUtils.getCurrentRequest();
        if (request != null) {
            return (MockUser) request.getAttribute(MockConstants.MOCK_USER_KEY);
        }
        return null;
    }

    /**
     * 验证用户操作
     *
     * @param targetUserName
     * @return
     */
    public static boolean validateUserUpdate(String targetUserName) {
        MockUser loginUser = getLoginUser();
        if (loginUser != null && StringUtils.isNotBlank(targetUserName)) {
            return ADMIN_USER.equals(loginUser.getUserName()) || loginUser.getUserName().equals(targetUserName);
        }
        return false;
    }

    /**
     * 获取可用的用户名
     *
     * @param queryUserName
     * @return
     */
    public static String getUserName(String queryUserName) {
        MockUser loginUser = getLoginUser();
        String userName = StringUtils.defaultIfBlank(queryUserName, loginUser != null ? loginUser.getUserName() : "");
        userName = SecurityUtils.validateUserUpdate(userName) ? userName : "";
        return userName;
    }
}
