package com.mengstudy.simple.mock;

import com.mengstudy.simple.mock.utils.MockJsUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Created on 2020/5/14 21:10 .<br>
 *
 * @author gary.fu
 */
@Slf4j
public class MockJsUtilsTest {

    @Test
    public void test() {
        String input = "{\n" +
                "    \"user|2\": [{\n" +
                "        'name': '@cname', \n" +
                "        'id|+1': 1\n" +
                "    }]\n" +
                "}";
        String output = MockJsUtils.mock(input);
        log.info("output: {}", output);
    }
}
