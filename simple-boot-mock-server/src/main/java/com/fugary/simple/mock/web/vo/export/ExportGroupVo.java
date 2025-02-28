package com.fugary.simple.mock.web.vo.export;

import com.fugary.simple.mock.entity.mock.MockGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Create date 2024/7/11<br>
 *
 * @author gary.fu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportGroupVo extends MockGroup {

    private List<ExportRequestVo> requests;

    private List<ExportSchemaVo> schemas;
}
