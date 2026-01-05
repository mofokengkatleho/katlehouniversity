package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.MonthlyReportDto;
import com.katlehouniversity.ecd.entity.Child;
import com.katlehouniversity.ecd.entity.Payment;
import com.katlehouniversity.ecd.repository.ChildRepository;
import com.katlehouniversity.ecd.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ChildRepository childRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public MonthlyReportDto generateMonthlyReport(Integer month, Integer year) {
        log.info("Generating monthly report for {}/{}", month, year);

        List<Child> activeChildren = childRepository.findByActiveTrue();
        List<Payment> monthPayments = paymentRepository.findByPaymentMonthAndPaymentYear(month, year);

        // Create a map of child ID to payment for quick lookup
        Map<Long, Payment> paymentMap = monthPayments.stream()
                .collect(Collectors.toMap(p -> p.getChild().getId(), p -> p));

        List<MonthlyReportDto.ChildPaymentStatus> paidChildren = new ArrayList<>();
        List<MonthlyReportDto.ChildPaymentStatus> owingChildren = new ArrayList<>();

        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalExpected = BigDecimal.ZERO;

        for (Child child : activeChildren) {
            totalExpected = totalExpected.add(child.getMonthlyFee());
            Payment payment = paymentMap.get(child.getId());

            MonthlyReportDto.ChildPaymentStatus status = MonthlyReportDto.ChildPaymentStatus.builder()
                    .childId(child.getId())
                    .fullName(child.getFullName())
                    .monthlyFee(child.getMonthlyFee())
                    .paymentReference(child.getPaymentReference())
                    .build();

            if (payment != null) {
                status.setAmountPaid(payment.getAmountPaid());
                status.setOutstanding(payment.getOutstandingAmount());
                status.setStatus(payment.getStatus().name());
                status.setPaymentDate(payment.getPaymentDate().toString());
                totalCollected = totalCollected.add(payment.getAmountPaid());

                if (payment.isFullyPaid()) {
                    paidChildren.add(status);
                } else {
                    owingChildren.add(status);
                }
            } else {
                status.setAmountPaid(BigDecimal.ZERO);
                status.setOutstanding(child.getMonthlyFee());
                status.setStatus("NOT_PAID");
                owingChildren.add(status);
            }
        }

        BigDecimal totalOutstanding = totalExpected.subtract(totalCollected);

        return MonthlyReportDto.builder()
                .month(month)
                .year(year)
                .period(YearMonth.of(year, month).toString())
                .paidChildren(paidChildren)
                .owingChildren(owingChildren)
                .totalCollected(totalCollected)
                .totalExpected(totalExpected)
                .totalOutstanding(totalOutstanding)
                .totalChildren((long) activeChildren.size())
                .paidCount((long) paidChildren.size())
                .owingCount((long) owingChildren.size())
                .build();
    }

    @Transactional(readOnly = true)
    public MonthlyReportDto getCurrentMonthReport() {
        YearMonth currentMonth = YearMonth.now();
        return generateMonthlyReport(currentMonth.getMonthValue(), currentMonth.getYear());
    }
}