package com.fugary.simple.mock.web.vo.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create date 2026/1/9<br>
 *
 * @author gary.fu
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MockHeaderVo {
    private boolean enabled;
    private String name;
    private String value;
}
