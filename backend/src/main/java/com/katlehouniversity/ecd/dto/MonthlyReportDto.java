package com.katlehouniversity.ecd.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {

    private Integer month;
    private Integer year;
    private String period;

    private List<ChildPaymentStatus> paidChildren;
    private List<ChildPaymentStatus> owingChildren;

    private BigDecimal totalCollected;
    private BigDecimal totalExpected;
    private BigDecimal totalOutstanding;

    private long totalChildren;
    private long paidCount;
    private long owingCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildPaymentStatus {
        private Long childId;
        private String fullName;
        private BigDecimal monthlyFee;
        private BigDecimal amountPaid;
        private BigDecimal outstanding;
        private String paymentReference;
        private String status;
        private String paymentDate;
    }
}