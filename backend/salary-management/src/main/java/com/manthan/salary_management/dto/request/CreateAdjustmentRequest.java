package com.manthan.salary_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdjustmentRequest {

    @NotBlank(message = "Type is mandatory")
    private String type;

    @NotNull(message = "Amount is mandatory")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Effective month is mandatory")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Must be in YYYY-MM format")
    private String effectiveMonth;

    private String note;
}
