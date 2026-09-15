package com.harmoniedev.api.report.domain.dto.response;

import com.harmoniedev.api.common.dto.CurrencyAmount;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * totalRevenue/totalExpenses are the default-currency (TND) totals, kept for simple callers.
 * revenueByCurrency/expensesByCurrency break the same figures down per currency so amounts in
 * different currencies are never silently summed together.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportOverviewResponse {
	private double totalRevenue;
	private double totalExpenses;
	private List<CurrencyAmount> revenueByCurrency;
	private List<CurrencyAmount> expensesByCurrency;
}
