package com.erp.finance.controller;

import com.erp.finance.dto.CashRunwayDto;
import com.erp.finance.dto.ProfitAndLossDto;
import com.erp.finance.service.ProfitAndLossService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class ProfitAndLossController {

    private final ProfitAndLossService pnlService;

    @GetMapping("/pnl")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ProfitAndLossDto> getProfitAndLoss(
            @RequestParam(defaultValue = "THIS_MONTH") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        LocalDate today = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;
        String title;

        switch (period.toUpperCase()) {
            case "TODAY" -> {
                start = today.atStartOfDay();
                end = today.atTime(LocalTime.MAX);
                title = "Hôm nay (" + today.toString() + ")";
            }
            case "THIS_WEEK" -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                start = monday.atStartOfDay();
                end = today.atTime(LocalTime.MAX);
                title = "Tuần này (Từ " + monday.toString() + ")";
            }
            case "THIS_QUARTER" -> {
                int firstMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
                LocalDate quarterStart = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
                start = quarterStart.atStartOfDay();
                end = today.atTime(LocalTime.MAX);
                title = "Quý " + (((today.getMonthValue() - 1) / 3) + 1) + "/" + today.getYear();
            }
            case "THIS_YEAR" -> {
                start = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                end = today.atTime(LocalTime.MAX);
                title = "Năm " + today.getYear();
            }
            case "CUSTOM" -> {
                start = startDate != null ? startDate : today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                end = endDate != null ? endDate : today.atTime(LocalTime.MAX);
                title = "Tùy chọn";
            }
            default -> { // THIS_MONTH
                start = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                end = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                title = "Tháng " + today.getMonthValue() + "/" + today.getYear();
            }
        }

        return ResponseEntity.ok(pnlService.calculatePnL(start, end, title));
    }

    @GetMapping("/cash-runway")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<CashRunwayDto> getCashRunway() {
        return ResponseEntity.ok(pnlService.calculateCashRunway());
    }
}
