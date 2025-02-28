package com.fugary.simple.mock.web.vo.result;

import com.fugary.simple.mock.entity.mock.MockSchema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Create date 2025/2/28<br>
 *
 * @author gary.fu
 */
@Data
public class MockSchemaResultVo implements Serializable {
    private List<MockSchema> schemas;
    private MockSchema componentSchema;
}
