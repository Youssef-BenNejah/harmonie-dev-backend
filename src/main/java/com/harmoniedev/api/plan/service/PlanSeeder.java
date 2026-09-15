package com.harmoniedev.api.plan.service;

import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Seeds the three default subscription plans on first startup so `GET /plans` is never empty. */
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
						.nom("Starter")
						.tagline("Pour démarrer sereinement")
						.prixMensuel(29)
						.prixAnnuel(290)
						.populaire(false)
						.fonctionnalites(List.of(
								"Jusqu'à 20 factures / mois", "2 utilisateurs", "1 entreprise", "Support par e-mail"))
						.build(),
				PlanDocument.builder()
						.nom("Pro")
						.tagline("Pour les équipes en croissance")
						.prixMensuel(69)
						.prixAnnuel(690)
						.populaire(true)
						.fonctionnalites(List.of(
								"Factures illimitées", "10 utilisateurs", "3 entreprises", "Rapports avancés", "Support prioritaire"))
						.build(),
				PlanDocument.builder()
						.nom("Entreprise")
						.tagline("Pour les organisations exigeantes")
						.prixMensuel(149)
						.prixAnnuel(1490)
						.populaire(false)
						.fonctionnalites(List.of(
								"Tout Pro inclus", "Utilisateurs illimités", "Entreprises illimitées", "Accès API", "Accompagnement dédié"))
						.build()));
		log.info("Seeded 3 default subscription plans");
	}
}
