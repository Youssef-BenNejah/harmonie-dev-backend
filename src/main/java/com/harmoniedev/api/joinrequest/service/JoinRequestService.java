package com.harmoniedev.api.joinrequest.service;

import com.harmoniedev.api.audit.domain.enums.AuditAction;
import com.harmoniedev.api.audit.service.AuditService;
import com.harmoniedev.api.auth.domain.dto.request.AdminCreateUserRequest;
import com.harmoniedev.api.auth.domain.dto.response.UserResponse;
import com.harmoniedev.api.auth.service.AuthService;
import com.harmoniedev.api.joinrequest.domain.dto.request.CreateJoinRequestRequest;
import com.harmoniedev.api.joinrequest.domain.dto.response.JoinRequestResponse;
import com.harmoniedev.api.joinrequest.domain.enums.JoinRequestStatus;
import com.harmoniedev.api.joinrequest.domain.model.JoinRequestDocument;
import com.harmoniedev.api.joinrequest.repository.JoinRequestRepository;
import com.harmoniedev.api.notification.service.NotificationService;
import com.harmoniedev.api.plan.domain.model.PlanDocument;
import com.harmoniedev.api.plan.repository.PlanRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JoinRequestService {
	private static final int DEFAULT_TRIAL_DAYS = 15;

	private final JoinRequestRepository repository;
	private final AuthService authService;
	private final AuditService auditService;
	private final NotificationService notificationService;
	private final PlanRepository planRepository;

	public JoinRequestService(
			JoinRequestRepository repository,
			AuthService authService,
			AuditService auditService,
			NotificationService notificationService,
			PlanRepository planRepository) {
		this.repository = repository;
		this.authService = authService;
		this.auditService = auditService;
		this.notificationService = notificationService;
		this.planRepository = planRepository;
	}

	public JoinRequestResponse submit(CreateJoinRequestRequest request, String ipAddress, String userAgent) {
		JoinRequestDocument doc = JoinRequestDocument.builder()
				.nom(request.getNom())
				.prenom(request.getPrenom())
				.email(request.getEmail().trim().toLowerCase())
				.telephone(request.getTelephone())
				.entreprise(request.getEntreprise())
				.message(request.getMessage())
				.requestedPlanId(request.getRequestedPlanId())
				.status(JoinRequestStatus.PENDING)
				.build();
		repository.save(doc);
		auditService.log(doc.getId(), AuditAction.JOIN_REQUEST_SUBMITTED, ipAddress, userAgent);
		notificationService.create(
				"join_request", doc.getId(), "Nouvelle demande d'adhésion",
				(request.getPrenom() + " " + request.getNom()).trim() + " (" + request.getEntreprise() + ") souhaite rejoindre la plateforme.",
				true);
		return toResponse(doc);
	}

	public List<JoinRequestResponse> list() {
		return repository.findAll().stream().map(this::toResponse).toList();
	}

	public JoinRequestResponse markContacted(String id) {
		JoinRequestDocument doc = findOrThrow(id);
		doc.setStatus(JoinRequestStatus.CONTACTED);
		repository.save(doc);
		return toResponse(doc);
	}

	public JoinRequestResponse reject(String id) {
		JoinRequestDocument doc = findOrThrow(id);
		doc.setStatus(JoinRequestStatus.REJECTED);
		repository.save(doc);
		return toResponse(doc);
	}

	/**
	 * Converts an accepted join request into a real tenant account: creates the User (via
	 * {@link AuthService}, which e-mails the generated credentials) and marks the request converted.
	 */
	public UserResponse convert(
			String id, Integer trialDays, String planIdOverride, String actorId, String ipAddress, String userAgent) {
		JoinRequestDocument doc = findOrThrow(id);
		if (doc.getStatus() == JoinRequestStatus.CONVERTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Join request already converted");
		}
		int days = trialDays != null ? trialDays : DEFAULT_TRIAL_DAYS;
		String resolvedPlanId = planIdOverride != null && !planIdOverride.isBlank() ? planIdOverride : doc.getRequestedPlanId();
		AdminCreateUserRequest createRequest = AdminCreateUserRequest.builder()
				.email(doc.getEmail())
				.firstName(doc.getPrenom())
				.lastName(doc.getNom())
				.planExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS))
				.planId(resolvedPlanId)
				.build();
		UserResponse user = authService.createUserByAdmin(createRequest, actorId, ipAddress, userAgent);
		doc.setStatus(JoinRequestStatus.CONVERTED);
		repository.save(doc);
		auditService.log(actorId, AuditAction.JOIN_REQUEST_CONVERTED, ipAddress, userAgent);
		return user;
	}

	private JoinRequestDocument findOrThrow(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
	}

	private JoinRequestResponse toResponse(JoinRequestDocument doc) {
		PlanDocument plan = doc.getRequestedPlanId() != null && !doc.getRequestedPlanId().isBlank()
				? planRepository.findById(doc.getRequestedPlanId()).orElse(null)
				: null;
		return JoinRequestResponse.builder()
				.id(doc.getId())
				.nom(doc.getNom())
				.prenom(doc.getPrenom())
				.email(doc.getEmail())
				.telephone(doc.getTelephone())
				.entreprise(doc.getEntreprise())
				.message(doc.getMessage())
				.requestedPlanId(doc.getRequestedPlanId())
				.requestedPlanNom(plan != null ? plan.getNom() : null)
				.status(doc.getStatus())
				.createdAt(doc.getCreatedAt())
				.build();
	}
}
