package com.erp.finance.service;

import com.erp.finance.dto.CashRunwayDto;
import com.erp.finance.dto.ProfitAndLossDto;
import com.erp.finance.dto.ProfitAndLossDto.CostCenterExpenseItem;
import com.erp.finance.model.CostCenter;
import com.erp.finance.repository.FinanceVoucherRepository;
import com.erp.finance.repository.TreasuryAccountRepository;
import com.erp.order.model.Order;
import com.erp.order.model.OrderStatus;
import com.erp.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitAndLossService {

    private final FinanceVoucherRepository voucherRepository;
    private final TreasuryAccountRepository accountRepository;
    private final OrderRepository orderRepository;

    /**
     * Báo cáo Tài chính Lãi Lỗ P&L Đa Kỳ (Income Statement)
     */
    @Transactional(readOnly = true)
    public ProfitAndLossDto calculatePnL(LocalDateTime startDate, LocalDateTime endDate, String periodTitle) {
        // 1. DOANH THU THUẦN (NET REVENUE)
        // Lấy doanh thu từ các phiếu thu bán hàng APPROVED hoặc đơn hàng hoàn tất
        BigDecimal salesVouchers = voucherRepository.sumTotalReceipts(startDate, endDate);
        BigDecimal grossRevenue = salesVouchers != null ? salesVouchers : BigDecimal.ZERO;

        // Nếu doanh thu từ phiếu thu rỗng, tính từ đơn hàng đã hoàn tất trong khoảng thời gian
        if (grossRevenue.compareTo(BigDecimal.ZERO) == 0) {
            List<Order> orders = orderRepository.findAll();
            for (Order o : orders) {
                if (o.getStatus() == OrderStatus.COMPLETED 
                        && o.getCreatedAt() != null 
                        && !o.getCreatedAt().isBefore(startDate) 
                        && !o.getCreatedAt().isAfter(endDate)) {
                    grossRevenue = grossRevenue.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
                }
            }
        }

        BigDecimal discounts = BigDecimal.ZERO;
        BigDecimal netRevenue = grossRevenue.subtract(discounts);

        // 2. GIÁ VỐN HÀNG BÁN (COGS) - Giả định ~65% doanh thu nếu chưa có phân hệ kho giá vốn riêng
        BigDecimal cogs = netRevenue.multiply(new BigDecimal("0.65")).setScale(2, RoundingMode.HALF_UP);

        // 3. LỢI NHUẬN GỘP (GROSS PROFIT)
        BigDecimal grossProfit = netRevenue.subtract(cogs);
        double grossMarginPercent = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(netRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        // 4. CHI PHÍ HOẠT ĐỘNG (OPEX BREAKDOWN THEO COST CENTER)
        List<Object[]> opexRaw = voucherRepository.aggregateExpensesByCostCenter(startDate, endDate);
        BigDecimal totalOpex = BigDecimal.ZERO;
        List<CostCenterExpenseItem> breakdown = new ArrayList<>();

        for (Object[] row : opexRaw) {
            CostCenter cc = (CostCenter) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Long count = (Long) row[2];
            totalOpex = totalOpex.add(amount);

            breakdown.add(CostCenterExpenseItem.builder()
                    .costCenterCode(cc.name())
                    .costCenterTitle(cc.getTitle())
                    .amount(amount)
                    .voucherCount(count != null ? count : 0L)
                    .build());
        }

        // Tính tỷ trọng % từng khoản chi trong tổng OPEX
        if (totalOpex.compareTo(BigDecimal.ZERO) > 0) {
            for (CostCenterExpenseItem item : breakdown) {
                double pct = item.getAmount().divide(totalOpex, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                item.setPercentageOfOpex(Math.round(pct * 10.0) / 10.0);
            }
        }

        // 5. LỢI NHUẬN RÒNG / THUẦN (EBIT / NET PROFIT)
        BigDecimal netProfit = grossProfit.subtract(totalOpex);
        double netMarginPercent = netRevenue.compareTo(BigDecimal.ZERO) > 0
                ? netProfit.divide(netRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        return ProfitAndLossDto.builder()
                .periodTitle(periodTitle != null ? periodTitle : "Kỳ báo cáo")
                .startDate(startDate)
                .endDate(endDate)
                .grossRevenue(grossRevenue)
                .discounts(discounts)
                .netRevenue(netRevenue)
                .cogs(cogs)
                .grossProfit(grossProfit)
                .grossMarginPercent(Math.round(grossMarginPercent * 10.0) / 10.0)
                .totalOpex(totalOpex)
                .opexBreakdown(breakdown)
                .netProfit(netProfit)
                .netMarginPercent(Math.round(netMarginPercent * 10.0) / 10.0)
                .build();
    }

    /**
     * Thuật toán Dự báo Thâm hụt Dòng tiền & Số ngày an toàn (Cash Runway Alert)
     */
    @Transactional(readOnly = true)
    public CashRunwayDto calculateCashRunway() {
        BigDecimal totalCash = accountRepository.getTotalBalance();
        BigDecimal cashOnHand = accountRepository.getTotalCashBalance();
        BigDecimal bankBalance = accountRepository.getTotalBankBalance();

        // Tính tổng chi phí trong 30 ngày qua để xác định Daily Burn Rate
        LocalDateTime since30Days = LocalDateTime.now().minusDays(30);
        BigDecimal monthlyBurnRate = voucherRepository.sumExpensesSince(since30Days);
        if (monthlyBurnRate == null || monthlyBurnRate.compareTo(BigDecimal.ZERO) <= 0) {
            monthlyBurnRate = new BigDecimal("30000000.00"); // Baseline tối thiểu 30 triệu/tháng
        }

        BigDecimal dailyBurnRate = monthlyBurnRate.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
        int runwayDays = dailyBurnRate.compareTo(BigDecimal.ZERO) > 0
                ? totalCash.divide(dailyBurnRate, 0, RoundingMode.FLOOR).intValue()
                : 999;

        String riskLevel;
        String riskTitle;
        String recommendation;

        if (runwayDays >= 90) {
            riskLevel = "SAFE";
            riskTitle = "🟢 Dòng tiền Vững chắc (An toàn trên 3 tháng)";
            recommendation = "Dòng tiền công ty đang duy trì mức đệm an toàn cao. Bạn có thể tự tin cân nhắc kế hoạch nhập thêm hàng tồn kho chiến lược hoặc mở rộng tiếp thị kinh doanh!";
        } else if (runwayDays >= 30) {
            riskLevel = "WARNING";
            riskTitle = "🟡 Cảnh báo Căng thẳng Dòng tiền (Đệm an toàn từ 1 - 3 tháng)";
            recommendation = "Dòng tiền đủ duy trì trong 1 đến 2 tháng tới. Khuyến nghị Kế toán trưởng đôn đốc thu hồi công nợ đại lý quá hạn và kiểm soát chặt các khoản chi phí không cấp thiết.";
        } else {
            riskLevel = "CRITICAL";
            riskTitle = "🔴 BÁO ĐỘNG ĐỎ: Nguy cơ cạn kiệt tiền mặt dưới 30 ngày!";
            recommendation = "Dòng tiền hiện tại chỉ đủ vận hành dưới 1 tháng! Yêu cầu Ban Giám đốc lập tức tạm hoãn các khoản chi chưa cấp thiết, khóa xuất hàng khách nợ xấu và tập trung thu hồi nợ!";
        }

        return CashRunwayDto.builder()
                .totalCashBalance(totalCash)
                .totalCashOnHand(cashOnHand)
                .totalBankBalance(bankBalance)
                .monthlyBurnRate(monthlyBurnRate)
                .dailyBurnRate(dailyBurnRate)
                .runwayDays(runwayDays)
                .riskLevel(riskLevel)
                .riskTitle(riskTitle)
                .recommendation(recommendation)
                .build();
    }
}
