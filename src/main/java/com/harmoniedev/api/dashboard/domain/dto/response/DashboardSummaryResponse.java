package com.harmoniedev.api.dashboard.domain.dto.response;

import com.harmoniedev.api.common.dto.CurrencyAmount;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * revenue/monthlyExpenses (and their trends) are the default-currency (TND) figures.
 * revenueByCurrency/expensesByCurrency break the same totals down per currency so amounts in
 * different currencies are never silently summed together.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
	private double revenue;
	private long unpaidInvoicesCount;
	private long clientsCount;
	private double monthlyExpenses;
	private List<Double> revenueTrend;
	private List<Long> unpaidTrend;
	private List<Long> clientsTrend;
	private List<Double> expensesTrend;
	private List<CurrencyAmount> revenueByCurrency;
	private List<CurrencyAmount> expensesByCurrency;
}
