package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Create date 2024/7/12<br>
 *
 * @author gary.fu
 */
@Component
public class SimpleMockGroupImporterImpl implements MockGroupImporter {

    public static final String SIMPLE_TYPE = "simple";

    @Override
    public String getType() {
        return SIMPLE_TYPE;
    }

    @Override
    public boolean match(String data) {
        if (StringUtils.isBlank(data)) {
            return false;
        }
        String trimmed = data.trim();
        if (trimmed.startsWith("{") && trimmed.contains("\"groups\"")) {
            return trimmed.contains("\"groupName\"") || trimmed.contains("\"groupPath\"") || trimmed.contains("\"requests\"");
        }
        return false;
    }

    @Override
    public ExportMockVo doImport(String data) {
        ExportMockVo mockVo = JsonUtils.fromJson(data, ExportMockVo.class);
        if (mockVo != null && mockVo.getGroups() != null) {
            return mockVo;
        }
        return null;
    }
}
