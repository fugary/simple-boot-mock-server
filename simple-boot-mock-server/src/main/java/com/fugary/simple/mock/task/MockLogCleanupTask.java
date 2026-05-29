package com.fugary.simple.mock.task;

import com.fugary.simple.mock.config.SimpleMockConfigProperties;
import com.fugary.simple.mock.service.mock.MockLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cleans expired Mock request logs.
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class MockLogCleanupTask implements SimpleMockScheduledTask {

    private static final String TASK_CODE = "mockLogCleanup";

    private static final String TASK_NAME_KEY = "mock.label.mockLogCleanupName";

    private static final String TASK_DESCRIPTION_KEY = "mock.label.mockLogCleanupDescription";

    private static final String TASK_CRON = "0 0 3 * * ?";

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final SimpleMockConfigProperties simpleMockConfigProperties;

    private final MockLogService mockLogService;

    private final ScheduledTaskRecorder scheduledTaskRecorder;

    public MockLogCleanupTask(SimpleMockConfigProperties simpleMockConfigProperties,
                              MockLogService mockLogService,
                              ScheduledTaskRecorder scheduledTaskRecorder) {
        this.simpleMockConfigProperties = simpleMockConfigProperties;
        this.mockLogService = mockLogService;
        this.scheduledTaskRecorder = scheduledTaskRecorder;
    }

    @Override
    public String getCode() {
        return TASK_CODE;
    }

    @Override
    public String getNameKey() {
        return TASK_NAME_KEY;
    }

    @Override
    public String getDescriptionKey() {
        return TASK_DESCRIPTION_KEY;
    }

    @Override
    public String getCron() {
        return TASK_CRON;
    }

    @Override
    public boolean isEnabled() {
        Integer retentionDays = simpleMockConfigProperties.getMockLogRetentionDays();
        return retentionDays != null && retentionDays > 0;
    }

    @Scheduled(cron = TASK_CRON)
    public void cleanupExpiredMockLogs() {
        execute(TRIGGER_TYPE_SCHEDULED);
    }

    @Override
    public ScheduledTaskExecuteResult execute(String triggerType) {
        ScheduledTaskExecuteResult result = createResult(triggerType);
        if (!running.compareAndSet(false, true)) {
            result.setSuccess(false);
            result.setSkipped(true);
            result.setMessageKey("mock.msg.scheduledTaskAlreadyRunning");
            finishAndRecord(result);
            return result;
        }
        try {
            cleanupExpiredMockLogs(result);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            log.error("Mock log cleanup failed", e);
        } finally {
            running.set(false);
            finishAndRecord(result);
        }
        return result;
    }

    private ScheduledTaskExecuteResult createResult(String triggerType) {
        ScheduledTaskExecuteResult result = new ScheduledTaskExecuteResult();
        result.setTaskCode(getCode());
        result.setTriggerType(triggerType);
        result.setStartDate(new Date());
        return result;
    }

    private void cleanupExpiredMockLogs(ScheduledTaskExecuteResult result) {
        Integer retentionDays = simpleMockConfigProperties.getMockLogRetentionDays();
        if (retentionDays == null || retentionDays <= 0) {
            result.setSuccess(true);
            result.setSkipped(true);
            result.setAffectedRows(0);
            result.setMessageKey("mock.msg.mockLogCleanupDisabled");
            log.debug("Skip Mock log cleanup, retention days: {}", retentionDays);
            return;
        }
        Date expiredDate = DateUtils.addDays(new Date(), -retentionDays);
        int removedCount = mockLogService.clearLogsBefore(expiredDate);
        result.setSuccess(true);
        result.setAffectedRows(removedCount);
        result.setMessageKey("mock.msg.mockLogCleanupCompleted");
        log.info("Mock log cleanup completed, retention days: {}, expired date: {}, removed count: {}",
                retentionDays, expiredDate, removedCount);
    }

    private void finishAndRecord(ScheduledTaskExecuteResult result) {
        Date endDate = new Date();
        result.setEndDate(endDate);
        result.setDuration(endDate.getTime() - result.getStartDate().getTime());
        scheduledTaskRecorder.record(result);
    }
}
