package com.manthan.salary_management.service;

import com.manthan.salary_management.config.RabbitMQConfig;
import com.manthan.salary_management.dto.message.PayrollCycleMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(PayrollCycleMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYROLL_EXCHANGE,
                RabbitMQConfig.PAYROLL_ROUTING_KEY,
                message
        );
        log.info("Published payroll message: runId={}, startBatch={}, retryCount={}",
                message.getPayrollCycleId(), message.getStartBatch(), message.getRetryCount());
    }
}
