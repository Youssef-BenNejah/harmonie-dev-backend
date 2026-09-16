package com.harmoniedev.api.plan.service;

import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.auth.repository.UserRepository;
import com.harmoniedev.api.catalog.domain.model.ProductDocument;
import com.harmoniedev.api.catalog.repository.ProductRepository;
import com.harmoniedev.api.client.domain.model.ClientDocument;
import com.harmoniedev.api.client.repository.ClientRepository;
import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import com.harmoniedev.api.currency.repository.CurrencyRepository;
import com.harmoniedev.api.exception.PlanLimitExceededException;
import com.harmoniedev.api.invoice.domain.model.InvoiceDocument;
import com.harmoniedev.api.invoice.repository.InvoiceRepository;
import com.harmoniedev.api.plan.domain.dto.response.PlanUsageResponse;
import com.harmoniedev.api.plan.domain.dto.response.PlanUsageResponse.LimitUsage;
import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import com.harmoniedev.api.tax.domain.model.TaxDocument;
import com.harmoniedev.api.tax.repository.TaxRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;

/**
 * Computes plan usage for a tenant and enforces plan limits/feature gates. All counts are scoped
 * per tenant via the `createdBy` field stamped on every resource at creation time.
 */
@Service
public class PlanUsageService {
	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final InvoiceRepository invoiceRepository;
	private final ClientRepository clientRepository;
	private final ProductRepository productRepository;
	private final TaxRepository taxRepository;
	private final CurrencyRepository currencyRepository;

	public PlanUsageService(
			UserRepository userRepository,
			PlanRepository planRepository,
			InvoiceRepository invoiceRepository,
			ClientRepository clientRepository,
			ProductRepository productRepository,
			TaxRepository taxRepository,
			CurrencyRepository currencyRepository) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.invoiceRepository = invoiceRepository;
		this.clientRepository = clientRepository;
		this.productRepository = productRepository;
		this.taxRepository = taxRepository;
		this.currencyRepository = currencyRepository;
	}

	public PlanDocument resolvePlan(String userId) {
		return userRepository.findById(userId)
				.map(UserDocument::getPlanId)
				.filter(id -> id != null && !id.isBlank())
				.flatMap(planRepository::findById)
				.orElseGet(this::fallbackPlan);
	}

	private PlanDocument fallbackPlan() {
		return planRepository.findByIsFreeTrialTrue()
				.orElseGet(() -> planRepository.findAll().stream().findFirst().orElse(null));
	}

	public PlanUsageResponse getUsage(String userId) {
		PlanDocument plan = resolvePlan(userId);
		UserDocument user = userRepository.findById(userId).orElse(null);

		int invoicesUsed = invoicesThisMonth(userId);
		int clientsUsed = countBy(clientRepository.findAll(), ClientDocument::getCreatedBy, userId);
		int productsUsed = countBy(productRepository.findAll(), ProductDocument::getCreatedBy, userId);
		int customTaxesUsed = (int) taxRepository.findAll().stream()
				.filter(t -> userId.equals(t.getCreatedBy()) && !t.isDefault())
				.count();
		int currenciesUsed = countBy(currencyRepository.findAll(), CurrencyDocument::getCreatedBy, userId);

		Integer trialDaysLeft = null;
		if (plan != null && plan.isFreeTrial() && user != null && user.getPlanExpiresAt() != null) {
			long days = java.time.Duration.between(Instant.now(), user.getPlanExpiresAt()).toDays();
			trialDaysLeft = (int) Math.max(0, days + 1);
		}

		return PlanUsageResponse.builder()
				.planId(plan != null ? plan.getId() : null)
				.planNom(plan != null ? plan.getNom() : null)
				.isFreeTrial(plan != null && plan.isFreeTrial())
				.planExpiresAt(user != null ? user.getPlanExpiresAt() : null)
				.trialDaysLeft(trialDaysLeft)
				.invoices(LimitUsage.of(invoicesUsed, plan != null ? plan.getMaxInvoicesPerMonth() : null))
				.clients(LimitUsage.of(clientsUsed, plan != null ? plan.getMaxClients() : null))
				.products(LimitUsage.of(productsUsed, plan != null ? plan.getMaxProducts() : null))
				.customTaxes(LimitUsage.of(customTaxesUsed, plan != null ? plan.getMaxCustomTaxes() : null))
				.currenciesUsed(currenciesUsed)
				.multiCurrency(plan == null || plan.isMultiCurrency())
				.reportsAccess(plan == null || plan.isReportsAccess())
				.expensesEnabled(plan == null || plan.isExpensesEnabled())
				.bulkExportEnabled(plan == null || plan.isBulkExportEnabled())
				.build();
	}

	public void assertCanCreateInvoice(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan == null || plan.getMaxInvoicesPerMonth() == null) return;
		int used = invoicesThisMonth(userId);
		if (used >= plan.getMaxInvoicesPerMonth()) {
			throw new PlanLimitExceededException(
					"Vous avez atteint la limite de " + plan.getMaxInvoicesPerMonth()
							+ " factures ce mois-ci pour le plan " + plan.getNom()
							+ ". Passez à un plan supérieur pour continuer.");
		}
	}

	public void assertCanCreateClient(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan == null || plan.getMaxClients() == null) return;
		int used = countBy(clientRepository.findAll(), ClientDocument::getCreatedBy, userId);
		if (used >= plan.getMaxClients()) {
			throw new PlanLimitExceededException(
					"Vous avez atteint la limite de " + plan.getMaxClients()
							+ " clients pour le plan " + plan.getNom()
							+ ". Passez à un plan supérieur pour continuer.");
		}
	}

	public void assertCanCreateProduct(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan == null || plan.getMaxProducts() == null) return;
		int used = countBy(productRepository.findAll(), ProductDocument::getCreatedBy, userId);
		if (used >= plan.getMaxProducts()) {
			throw new PlanLimitExceededException(
					"Vous avez atteint la limite de " + plan.getMaxProducts()
							+ " services/produits pour le plan " + plan.getNom()
							+ ". Passez à un plan supérieur pour continuer.");
		}
	}

	public void assertCanCreateCustomTax(String userId, boolean isDefaultTax) {
		if (isDefaultTax) return;
		PlanDocument plan = resolvePlan(userId);
		if (plan == null || plan.getMaxCustomTaxes() == null) return;
		int used = (int) taxRepository.findAll().stream()
				.filter(t -> userId.equals(t.getCreatedBy()) && !t.isDefault())
				.count();
		if (used >= plan.getMaxCustomTaxes()) {
			throw new PlanLimitExceededException(plan.getMaxCustomTaxes() == 0
					? "Votre plan " + plan.getNom() + " ne permet pas d'ajouter de taxe personnalisée. Passez à un plan supérieur."
					: "Vous avez atteint la limite de " + plan.getMaxCustomTaxes()
							+ " taxes personnalisées pour le plan " + plan.getNom() + ".");
		}
	}

	public void assertCanCreateCurrency(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan == null || plan.isMultiCurrency()) return;
		int used = countBy(currencyRepository.findAll(), CurrencyDocument::getCreatedBy, userId);
		if (used >= 1) {
			throw new PlanLimitExceededException(
					"Votre plan " + plan.getNom() + " ne permet qu'une seule devise. Passez à un plan supérieur pour le multi-devises.");
		}
	}

	public void assertReportsAccess(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan != null && !plan.isReportsAccess()) {
			throw new PlanLimitExceededException(
					"Les rapports ne sont pas inclus dans votre plan " + plan.getNom() + ". Passez à un plan supérieur pour y accéder.");
		}
	}

	public void assertExpensesEnabled(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan != null && !plan.isExpensesEnabled()) {
			throw new PlanLimitExceededException(
					"Le suivi des dépenses n'est pas inclus dans votre plan " + plan.getNom() + ". Passez à un plan supérieur pour y accéder.");
		}
	}

	public void assertBulkExportEnabled(String userId) {
		PlanDocument plan = resolvePlan(userId);
		if (plan != null && !plan.isBulkExportEnabled()) {
			throw new PlanLimitExceededException(
					"L'export ZIP en masse n'est pas inclus dans votre plan " + plan.getNom() + ". Passez à un plan supérieur pour y accéder.");
		}
	}

	private int invoicesThisMonth(String userId) {
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		Instant monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
		return (int) invoiceRepository.findAll().stream()
				.filter(inv -> userId.equals(inv.getCreatedBy()))
				.map(InvoiceDocument::getCreated)
				.filter(created -> created != null && !created.isBefore(monthStart))
				.count();
	}

	private <T> int countBy(java.util.List<T> all, java.util.function.Function<T, String> createdByFn, String userId) {
		return (int) all.stream().filter(t -> userId.equals(createdByFn.apply(t))).count();
	}
}
