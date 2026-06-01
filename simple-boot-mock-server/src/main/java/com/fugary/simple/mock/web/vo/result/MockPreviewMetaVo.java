package com.fugary.simple.mock.web.vo.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fugary.simple.mock.entity.mock.MockLog;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MockPreviewMetaVo {

    private String headers;

    private String diagnoseData;

    @JsonIgnore
    private String diagnoseId;

    @JsonIgnore
    private String userName;

    @JsonIgnore
    private String mockGroupPath;

    public static MockPreviewMetaVo of(MockLog mockLog) {
        if (mockLog == null || mockLog.getDiagnoseId() == null) {
            return null;
        }
        return MockPreviewMetaVo.builder()
                .headers(mockLog.getHeaders())
                .diagnoseData(mockLog.getDiagnoseData())
                .diagnoseId(mockLog.getDiagnoseId())
                .userName(mockLog.getUserName())
                .mockGroupPath(mockLog.getMockGroupPath())
                .build();
    }
}
