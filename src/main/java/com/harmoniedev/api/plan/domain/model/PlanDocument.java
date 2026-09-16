package com.harmoniedev.api.plan.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDocument {
	@Id
	private String id;
	private String nom;
	private String tagline;
	private double prixMensuel;
	private double prixAnnuel;
	@Default
	private boolean populaire = false;
	@Default
	private List<String> fonctionnalites = List.of();

	/** Marks the single plan auto-assigned to brand-new tenants (registration / join-request conversion). */
	@Default
	private boolean isFreeTrial = false;

	/** Only meaningful when isFreeTrial=true — how many days the trial lasts before planExpiresAt. */
	private Integer trialDurationDays;

	/** All limits below: null = unlimited. Usage is counted per tenant (createdBy). */
	private Integer maxInvoicesPerMonth;
	private Integer maxClients;
	private Integer maxProducts;

	/** maxCustomTaxes counts only non-default Tax entries; the single default tax never counts against it. */
	private Integer maxCustomTaxes;

	@Default
	private boolean multiCurrency = true;

	@Default
	private boolean reportsAccess = true;

	@Default
	private boolean expensesEnabled = true;

	@Default
	private boolean bulkExportEnabled = true;
}
