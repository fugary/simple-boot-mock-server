package com.fugary.simple.mock.task;

/**
 * Common contract for scheduled jobs that can also be triggered manually.
 *
 * @author gary.fu
 */
public interface SimpleMockScheduledTask {

    String TRIGGER_TYPE_SCHEDULED = "SCHEDULED";

    String TRIGGER_TYPE_MANUAL = "MANUAL";

    String getCode();

    String getNameKey();

    String getDescriptionKey();

    String getCron();

    boolean isEnabled();

    ScheduledTaskExecuteResult execute(String triggerType);
}
