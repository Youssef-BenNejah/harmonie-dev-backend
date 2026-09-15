package com.harmoniedev.api.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDocument {
	@Id
	private String id;
	private String invoiceId;
	private double amountPaid;

	/** "Virement bancaire" | "Espèces" | "Autres" */
	private String paymentMethod;

	private String paymentDate;
	private String createdBy;
}
