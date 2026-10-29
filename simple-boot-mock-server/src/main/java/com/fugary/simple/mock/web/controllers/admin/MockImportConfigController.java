package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class MockImportConfigController {
    private final SimpleMockConfigProperties properties;

    public MockImportConfigController(SimpleMockConfigProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/admin/importConfig")
    public SimpleResult<Map<String, Long>> importConfig() {
        return SimpleResultUtils.createSimpleResult(Map.of(
                "maxFileSize", properties.getMaxUploadFileSize(),
                "maxRequestSize", properties.getMaxUploadSize()));
    }
}
