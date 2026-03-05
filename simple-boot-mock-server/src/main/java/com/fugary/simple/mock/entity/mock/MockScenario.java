package com.fugary.simple.mock.entity.mock;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Mock scenario bound to one mock group.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MockScenario extends MockBase {

    private static final long serialVersionUID = -7059778267480586682L;

    private Integer groupId;

    private String scenarioName;

    private String scenarioCode;

    private String description;
}
