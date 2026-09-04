package com.manthan.salary_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCycleResponse {
    private Long id;
    private String month;
    private String triggeredBy;
    private LocalDateTime runAt;
    private String status;
    private List<PaySlipResponse> paySlips;
}
