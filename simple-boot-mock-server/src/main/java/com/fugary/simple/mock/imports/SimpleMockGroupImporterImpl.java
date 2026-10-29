package com.fugary.simple.mock.imports;

import com.fugary.simple.mock.utils.JsonUtils;
import com.fugary.simple.mock.web.vo.export.ExportMockVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.io.IOException;

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
    public ExportMockVo doImportStream(InputStream input) throws IOException {
        try {
            ExportMockVo result = JsonUtils.getMapper().readValue(input, ExportMockVo.class);
            return result != null && result.getGroups() != null ? result : null;
        } catch (JsonProcessingException e) {
            return null; // 交由服务层保留原有格式诊断
        }
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
