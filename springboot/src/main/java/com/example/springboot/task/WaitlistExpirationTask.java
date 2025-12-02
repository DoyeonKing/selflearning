package com.example.springboot.task;

import com.example.springboot.service.WaitlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 候补超时处理定时任务
 * 每1分钟执行一次，检查状态为 notified 的候补记录，如果通知发送时间超过15分钟仍未支付，自动将状态更新为 expired
 */
@Component
public class WaitlistExpirationTask {

    private static final Logger logger = LoggerFactory.getLogger(WaitlistExpirationTask.class);

    private final WaitlistService waitlistService;

    @Autowired
    public WaitlistExpirationTask(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    /**
     * 候补超时处理任务
     * 每1分钟执行一次（60000毫秒）
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void checkExpiredWaitlists() {
        try {
            logger.debug("开始执行候补超时处理任务");
            waitlistService.expireNotifiedWaitlists();
            logger.debug("候补超时处理任务执行完成");
        } catch (Exception e) {
            logger.error("候补超时处理任务执行失败", e);
        }
    }
}

