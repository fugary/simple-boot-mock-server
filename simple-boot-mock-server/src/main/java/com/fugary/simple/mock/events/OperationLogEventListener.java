package com.fugary.simple.mock.events;

import com.fugary.simple.mock.service.mock.MockLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Create date 2024/11/14<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class OperationLogEventListener implements ApplicationListener<OperationLogEvent> {

    @Autowired
    private MockLogService mockLogService;

    @Async
    @Override
    public void onApplicationEvent(OperationLogEvent event) {
        try {
            log.info("{}", event.getLog());
            mockLogService.save(event.getLog());
        } catch (Exception e) {
            log.error("操作日志保存失败", e);
        }
    }
}
