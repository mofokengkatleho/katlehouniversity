package com.katlehouniversity.ecd.controller;

import com.katlehouniversity.ecd.entity.Payment;
import com.katlehouniversity.ecd.entity.Transaction;
import com.katlehouniversity.ecd.service.PaymentMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TransactionController {

    private final PaymentMatchingService paymentMatchingService;

    @GetMapping("/unmatched")
    public ResponseEntity<List<Transaction>> getUnmatchedTransactions() {
        return ResponseEntity.ok(paymentMatchingService.getUnmatchedTransactions());
    }

    @GetMapping("/unmatched/count")
    public ResponseEntity<Long> getUnmatchedCount() {
        return ResponseEntity.ok(paymentMatchingService.getUnmatchedTransactionCount());
    }

    @PostMapping("/match-all")
    public ResponseEntity<Void> matchAllTransactions() {
        paymentMatchingService.matchAllUnmatchedTransactions();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{transactionId}/match")
    public ResponseEntity<Payment> manuallyMatchTransaction(
            @PathVariable Long transactionId,
            @RequestParam Long childId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        Payment payment = paymentMatchingService.manuallyMatchTransaction(
                transactionId, childId, month, year);
        return ResponseEntity.ok(payment);
    }
}
