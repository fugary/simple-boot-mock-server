package com.fugary.simple.mock.web.vo.query;

import lombok.Data;

@Data
public class MockScenarioQueryVo extends SimpleQueryVo {

    private static final long serialVersionUID = -8064724830167153881L;

    private Integer groupId;

    private String scenarioCode;
}
