package com.harmoniedev.api.invoice.domain.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocument {
	@Id
	private String id;
	private String clientId;
	private String currencyId;
	private int number;
	private int year;

	/** "Facture" | "Devis" | "Bon de livraison" — the printed document type, not a workflow state. */
	private String status;

	/** "impayé" | "Partiellement payé" | "Payé" | "Retard" — server-derived from paidAmount/total/dueDate. */
	@Default
	private String paymentStatus = "impayé";

	/** "Standard" | "Proforma" — immutable after creation. */
	private String type;

	@Default
	private boolean isConverted = false;

	private String date;
	private String expirationDate;
	private String note;

	@Default
	private List<InvoiceItemDocument> items = List.of();

	private double timbre;
	private double subtotal;
	private double taxAmount;
	private double total;

	@Default
	private double paidAmount = 0;

	private String createdBy;

	@CreatedDate
	private Instant created;

	private String factureImage;
}
