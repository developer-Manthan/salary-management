package com.manthan.salary_management.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCycleMessage {
    private Long payrollCycleId;
    private String month;
    private String triggerType;

    @Builder.Default
    private int startBatch = 0;

    @Builder.Default
    private int retryCount = 0;
}
