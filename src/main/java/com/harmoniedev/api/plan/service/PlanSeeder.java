package com.harmoniedev.api.plan.service;

import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Seeds the free-trial plan + three default subscription plans on first startup. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlanSeeder {
	private final PlanRepository repository;

	@EventListener(ApplicationReadyEvent.class)
	public void seedPlans() {
		if (repository.count() > 0) {
			return;
		}
		repository.saveAll(List.of(
				PlanDocument.builder()
						.nom("Essai gratuit")
						.tagline("Découvrez Harmonie-dev sans engagement")
						.prixMensuel(0)
						.prixAnnuel(0)
						.populaire(false)
						.isFreeTrial(true)
						.trialDurationDays(15)
						.maxInvoicesPerMonth(10)
						.maxClients(10)
						.maxProducts(15)
						.maxCustomTaxes(0)
						.multiCurrency(false)
						.reportsAccess(true)
						.expensesEnabled(true)
						.bulkExportEnabled(false)
						.fonctionnalites(List.of(
								"15 jours d'essai", "Jusqu'à 10 factures / mois", "Jusqu'à 10 clients",
								"Devise unique (TND)", "Rapports inclus"))
						.build(),
				PlanDocument.builder()
						.nom("Starter")
						.tagline("Pour démarrer sereinement")
						.prixMensuel(29)
						.prixAnnuel(290)
						.populaire(false)
						.isFreeTrial(false)
						.maxInvoicesPerMonth(20)
						.maxClients(30)
						.maxProducts(20)
						.maxCustomTaxes(0)
						.multiCurrency(false)
						.reportsAccess(false)
						.expensesEnabled(false)
						.bulkExportEnabled(false)
						.fonctionnalites(List.of(
								"Jusqu'à 20 factures / mois", "Jusqu'à 30 clients", "Jusqu'à 20 services",
								"Devise unique (TND)", "Support par e-mail"))
						.build(),
				PlanDocument.builder()
						.nom("Pro")
						.tagline("Pour les équipes en croissance")
						.prixMensuel(69)
						.prixAnnuel(690)
						.populaire(true)
						.isFreeTrial(false)
						.maxInvoicesPerMonth(null)
						.maxClients(null)
						.maxProducts(null)
						.maxCustomTaxes(null)
						.multiCurrency(true)
						.reportsAccess(true)
						.expensesEnabled(true)
						.bulkExportEnabled(true)
						.fonctionnalites(List.of(
								"Factures illimitées", "Clients illimités", "Multi-devises",
								"Rapports avancés", "Export ZIP en masse", "Suivi des dépenses", "Support prioritaire"))
						.build(),
				PlanDocument.builder()
						.nom("Entreprise")
						.tagline("Pour les organisations exigeantes")
						.prixMensuel(149)
						.prixAnnuel(1490)
						.populaire(false)
						.isFreeTrial(false)
						.maxInvoicesPerMonth(null)
						.maxClients(null)
						.maxProducts(null)
						.maxCustomTaxes(null)
						.multiCurrency(true)
						.reportsAccess(true)
						.expensesEnabled(true)
						.bulkExportEnabled(true)
						.fonctionnalites(List.of(
								"Tout Pro inclus", "Utilisateurs illimités", "Entreprises illimitées",
								"Accès API", "Accompagnement dédié"))
						.build()));
		log.info("Seeded free-trial plan + 3 default subscription plans");
	}
}
