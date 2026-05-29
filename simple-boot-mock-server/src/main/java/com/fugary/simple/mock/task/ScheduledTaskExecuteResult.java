package com.fugary.simple.mock.task;

import lombok.Data;

import java.util.Date;

/**
 * Last execution snapshot of a scheduled task.
 *
 * @author gary.fu
 */
@Data
public class ScheduledTaskExecuteResult {

    private String taskCode;

    private String triggerType;

    private boolean success;

    private boolean skipped;

    private String message;

    private String messageKey;

    private Integer affectedRows;

    private Date startDate;

    private Date endDate;

    private Long duration;
}
