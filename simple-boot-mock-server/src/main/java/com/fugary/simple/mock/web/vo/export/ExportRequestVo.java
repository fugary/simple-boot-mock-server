package com.fugary.simple.mock.web.vo.export;

import com.fugary.simple.mock.entity.mock.MockRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Create date 2024/7/11<br>
 *
 * @author gary.fu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExportRequestVo extends MockRequest {

    private List<ExportDataVo> dataList;

    private List<ExportSchemaVo> schemas = new ArrayList<>();
}
