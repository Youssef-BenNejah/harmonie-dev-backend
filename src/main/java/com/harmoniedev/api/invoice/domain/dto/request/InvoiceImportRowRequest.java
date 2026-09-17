package com.harmoniedev.api.invoice.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of a bulk invoice import — a historical invoice re-entered without line-item detail. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceImportRowRequest {
	/** 1-based row number in the uploaded file, for error reporting. */
	private int line;

	/** Existing client's display name — matched against the tenant's own client list. */
	@NotBlank
	private String client;

	/** yyyy-MM-dd */
	@NotBlank
	private String date;

	/** Currency code (e.g. "TND") — matched against the tenant's own currencies. */
	@NotBlank
	private String devise;

	private double montant;

	private Double montantPaye;

	/** "impayé" | "Partiellement payé" | "Payé" | "Retard" — defaults to "impayé" if blank/unrecognized. */
	private String statut;

	/** "Standard" | "Proforma" — defaults to "Standard". */
	private String type;

	/** Reuses this number instead of auto-incrementing, so an old system's numbering can be preserved. */
	private Integer numero;

	private String note;
}
