package com.harmoniedev.api.payment.domain.dto.response;

import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
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
public class PaymentResponse {
	private String id;
	private InvoiceSnapshotResponse invoice;
	private double amountPaid;
	private String paymentMethod;
	private String paymentDate;
	private String createdBy;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class InvoiceSnapshotResponse {
		private String id;
		private int number;
		private int year;
		private String type;
		private double total;
		private CurrencyResponse currency;
	}
}
