package com.fugary.simple.mock.events;

import com.fugary.simple.mock.entity.mock.MockLog;
import org.springframework.context.ApplicationEvent;

/**
 * Create date 2024/11/14<br>
 *
 * @author gary.fu
 */
public class OperationLogEvent extends ApplicationEvent {

    public OperationLogEvent(MockLog source) {
        super(source);
    }

    public MockLog getLog() {
        return (MockLog) getSource();
    }
}
