package com.fugary.simple.mock.web.vo.result;

import com.fugary.simple.mock.task.ScheduledTaskExecuteResult;
import com.fugary.simple.mock.task.SimpleMockScheduledTask;
import lombok.Data;

/**
 * Scheduled task view object.
 *
 * @author gary.fu
 */
@Data
public class ScheduledTaskVo {

    private String code;

    private String nameKey;

    private String descriptionKey;

    private String cron;

    private boolean enabled;

    private ScheduledTaskExecuteResult lastResult;

    public ScheduledTaskVo(SimpleMockScheduledTask task, ScheduledTaskExecuteResult lastResult) {
        this.code = task.getCode();
        this.nameKey = task.getNameKey();
        this.descriptionKey = task.getDescriptionKey();
        this.cron = task.getCron();
        this.enabled = task.isEnabled();
        this.lastResult = lastResult;
    }
}
