package com.manthan.salary_management.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TopEarnerResponse {
    private Long employeeId;
    private String employeeCode;
    private String name;
    private String department;
    private String jobTitle;
    private String country;
    private BigDecimal currentSalary;
}
