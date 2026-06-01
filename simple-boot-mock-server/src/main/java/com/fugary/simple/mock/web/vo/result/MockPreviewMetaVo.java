package com.fugary.simple.mock.web.vo.result;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MockPreviewMetaVo {

    private String headers;

    private String diagnoseData;
}
