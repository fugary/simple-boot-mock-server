package com.fugary.simple.mock.web.controllers.admin;

import com.fugary.simple.mock.contants.MockErrorConstants;
import com.fugary.simple.mock.task.ScheduledTaskExecuteResult;
import com.fugary.simple.mock.task.ScheduledTaskRecorder;
import com.fugary.simple.mock.task.SimpleMockScheduledTask;
import com.fugary.simple.mock.utils.SimpleResultUtils;
import com.fugary.simple.mock.utils.security.SecurityUtils;
import com.fugary.simple.mock.web.vo.SimpleResult;
import com.fugary.simple.mock.web.vo.result.ScheduledTaskVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Scheduled task management APIs.
 *
 * @author gary.fu
 */
@RestController
@RequestMapping("/admin/scheduled-tasks")
public class ScheduledTaskController {

    private final List<SimpleMockScheduledTask> scheduledTasks;

    private final ScheduledTaskRecorder scheduledTaskRecorder;

    public ScheduledTaskController(List<SimpleMockScheduledTask> scheduledTasks,
                                   ScheduledTaskRecorder scheduledTaskRecorder) {
        this.scheduledTasks = scheduledTasks;
        this.scheduledTaskRecorder = scheduledTaskRecorder;
    }

    @GetMapping
    public SimpleResult<List<ScheduledTaskVo>> search() {
        if (!SecurityUtils.isAdminUser()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, null);
        }
        List<ScheduledTaskVo> result = scheduledTasks.stream()
                .sorted(Comparator.comparing(SimpleMockScheduledTask::getCode))
                .map(task -> new ScheduledTaskVo(task, scheduledTaskRecorder.getLastResult(task.getCode())))
                .collect(Collectors.toList());
        return SimpleResultUtils.createSimpleResult(result);
    }

    @PostMapping("/{taskCode}/trigger")
    public SimpleResult<ScheduledTaskExecuteResult> trigger(@PathVariable String taskCode) {
        if (!SecurityUtils.isAdminUser()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_403, null);
        }
        Optional<SimpleMockScheduledTask> taskOptional = scheduledTasks.stream()
                .filter(task -> task.getCode().equals(taskCode))
                .findFirst();
        if (!taskOptional.isPresent()) {
            return SimpleResultUtils.createSimpleResult(MockErrorConstants.CODE_404, null);
        }
        return SimpleResultUtils.createSimpleResult(taskOptional.get().execute(SimpleMockScheduledTask.TRIGGER_TYPE_MANUAL));
    }
}
