package com.harmoniedev.api.invoice.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
public class InvoiceRequest {
	@NotBlank
	private String clientId;

	@NotBlank
	private String currencyId;

	/** "Facture" | "Devis" | "Bon de livraison" */
	@NotBlank
	private String status;

	/** "Standard" | "Proforma" — only read on create; immutable afterwards. */
	private String type;

	@NotBlank
	private String date;

	@NotBlank
	private String expirationDate;

	private String note;

	private double timbre;

	@NotNull
	@NotEmpty
	@Valid
	private List<InvoiceItemRequest> items;

	private String factureImage;
}
