package com.manthan.salary_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentResponse {
    private Long id;
    private String type;
    private BigDecimal amount;
    private String effectiveMonth;
    private String note;
    private LocalDateTime createdAt;
}
