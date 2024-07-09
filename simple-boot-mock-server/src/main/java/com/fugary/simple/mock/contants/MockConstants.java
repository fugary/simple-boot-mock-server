package com.fugary.simple.mock.contants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Created on 2020/5/4 9:19 .<br>
 *
 * @author gary.fu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MockConstants {

    public static final String MOCK_USER_KEY = "simple-mock-user";

    public static final String MOCK_LOCALE_KEY = "locale";

    public static final String MOCK_PREFIX = "/mock";

    public static final String ALL_PATH_PATTERN = "/**";

    public static final String FILTER_PATH_PATTERN = "/*";

    public static final String SIMPLE_BOOT_MOCK_HEADER = "simple-boot-mock";

    public static final String MOCK_DATA_ID_HEADER = "mock-data-id";

    public static final String MOCK_DATA_PREVIEW_HEADER = "mock-data-preview";

    public static final String MOCK_DATA_MATCH_PATTERN_HEADER = "mock-data-match-pattern";

    public static final String MOCK_DATA_PATH_PARAMS_HEADER = "mock-data-path-params";

    public static final String MOCK_META_DATA_REQ = "mock-meta-req";

}
