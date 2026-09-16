package com.harmoniedev.api.plan.domain.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUsageResponse {
	private String planId;
	private String planNom;
	private boolean isFreeTrial;
	private Instant planExpiresAt;

	/** Only set when the current plan is the free trial. */
	private Integer trialDaysLeft;

	private LimitUsage invoices;
	private LimitUsage clients;
	private LimitUsage products;
	private LimitUsage customTaxes;

	private int currenciesUsed;
	private boolean multiCurrency;
	private boolean reportsAccess;
	private boolean expensesEnabled;
	private boolean bulkExportEnabled;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class LimitUsage {
		private int used;

		/** null = unlimited. */
		private Integer limit;

		public static LimitUsage of(int used, Integer limit) {
			return LimitUsage.builder().used(used).limit(limit).build();
		}
	}
}
