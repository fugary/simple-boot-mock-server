package com.mengstudy.simple.mock.contants;

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

    public static final int CODE_404 = 404; // 数据不存在

    public static final int CODE_1001 = 1001; // 数据已经存在

}
