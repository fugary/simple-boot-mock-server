package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import org.springframework.stereotype.Component;

/**
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
@Component
public class SimpleMockGroupImporterImpl implements MockGroupImporter {

    @Override
    public boolean isSupport(String type) {
        return "simple".equals(type);
    }

    @Override
    public ExportMockVo doImport(String data) {
        return JsonUtils.fromJson(data, ExportMockVo.class);
    }
}
