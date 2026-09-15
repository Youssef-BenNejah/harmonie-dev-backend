package com.harmoniedev.api.payment.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
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
public class PaymentRequest {
	@NotBlank
	private String invoiceId;

	@Positive
	private double amountPaid;

	@NotBlank
	private String paymentMethod;

	@NotBlank
	private String paymentDate;
}
