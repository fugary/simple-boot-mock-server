package com.fugary.simple.mock.task;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps in-memory execution snapshots for scheduled tasks.
 *
 * @author gary.fu
 */
@Component
public class ScheduledTaskRecorder {

    private final Map<String, ScheduledTaskExecuteResult> lastResultMap = new ConcurrentHashMap<>();

    public void record(ScheduledTaskExecuteResult result) {
        if (result != null && result.getTaskCode() != null) {
            lastResultMap.put(result.getTaskCode(), result);
        }
    }

    public ScheduledTaskExecuteResult getLastResult(String taskCode) {
        return lastResultMap.get(taskCode);
    }
}
