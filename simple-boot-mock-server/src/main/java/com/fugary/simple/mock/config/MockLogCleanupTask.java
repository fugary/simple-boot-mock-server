package com.fugary.simple.mock.config;

import com.fugary.simple.mock.service.mock.MockLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 定时清理过期Mock请求日志。
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class MockLogCleanupTask {

    @Autowired
    private SimpleMockConfigProperties simpleMockConfigProperties;

    @Autowired
    private MockLogService mockLogService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredMockLogs() {
        Integer retentionDays = simpleMockConfigProperties.getMockLogRetentionDays();
        if (retentionDays == null || retentionDays <= 0) {
            log.debug("跳过Mock日志清理，当前保留天数：{}", retentionDays);
            return;
        }
        Date expiredDate = DateUtils.addDays(new Date(), -retentionDays);
        try {
            int removedCount = mockLogService.clearLogsBefore(expiredDate);
            log.info("Mock日志清理完成，保留天数：{}，清理截止时间：{}，清理数量：{}", retentionDays, expiredDate, removedCount);
        } catch (Exception e) {
            log.error("Mock日志清理失败", e);
        }
    }
}
