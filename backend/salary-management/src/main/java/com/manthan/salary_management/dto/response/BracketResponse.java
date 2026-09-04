package com.manthan.salary_management.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BracketResponse {
    private List<BracketEntry> brackets;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BracketEntry {
        private String range;
        private Long count;
        private BigDecimal percentage;
    }
}
