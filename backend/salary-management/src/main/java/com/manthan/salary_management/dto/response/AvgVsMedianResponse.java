package com.manthan.salary_management.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AvgVsMedianResponse {
    private BigDecimal average;
    private BigDecimal median;
    private BigDecimal difference;
    private String skewDirection;
}
