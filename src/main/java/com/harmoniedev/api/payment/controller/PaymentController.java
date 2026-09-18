package com.harmoniedev.api.payment.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.common.paging.Paging;
import com.harmoniedev.api.payment.domain.dto.request.PaymentRequest;
import com.harmoniedev.api.payment.domain.dto.response.PaymentResponse;
import com.harmoniedev.api.payment.service.PaymentService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
	private final PaymentService service;

	public PaymentController(PaymentService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> create(
			@Valid @RequestBody PaymentRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Payment recorded", service.create(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PaymentResponse>>> list(
			@RequestParam(required = false) String invoiceId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		if (invoiceId != null) {
			return ResponseEntity.ok(ApiResponse.success("Payments", service.listForInvoice(invoiceId)));
		}
		return Paging.respond("Payments", service.listPage(Paging.of(page, size)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<PaymentResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Payment", service.get(id)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Payment deleted", null));
	}
}
