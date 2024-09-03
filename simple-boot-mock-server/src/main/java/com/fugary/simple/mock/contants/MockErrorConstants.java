package com.fugary.simple.mock.contants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created on 2020/5/4 9:53 .<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockErrorConstants {

    public static final int CODE_0 = 0; // 操作成功

    public static final int CODE_1 = 1; // 操作失败

    public static final int CODE_400 = 400; // 请求错误

    public static final int CODE_401 = 401; // 认证失败

    public static final int CODE_404 = 404; // 数据不存在

    public static final int CODE_403 = 403; // 没有权限访问

    public static final int CODE_500 = 500; // 服务器内部错误

    public static final int CODE_1001 = 1001; // 数据已经存在
    /**
     * 用户不存在或密码不正确
     */
    public static final int CODE_2001 = 2001;
    /**
     * 上传文件不能为空
     */
    public static final int CODE_2002 = 2002;
    /**
     * 上传文件解析失败
     */
    public static final int CODE_2003 = 2003;
    /**
     * 导入的分组路径已存在
     */
    public static final int CODE_2004 = 2004;
}
