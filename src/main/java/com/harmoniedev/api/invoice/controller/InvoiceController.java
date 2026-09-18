package com.harmoniedev.api.invoice.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.common.paging.Paging;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceImportRequest;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceRequest;
import com.harmoniedev.api.invoice.domain.dto.request.SendInvoiceRequest;
import com.harmoniedev.api.invoice.domain.dto.response.DocumentUploadResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceImportResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.harmoniedev.api.invoice.service.InvoiceService;
import com.harmoniedev.api.payment.domain.dto.response.PaymentResponse;
import com.harmoniedev.api.payment.service.PaymentService;
import com.harmoniedev.api.plan.service.PlanUsageService;
import com.harmoniedev.api.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {
	private final InvoiceService service;
	private final PaymentService paymentService;
	private final PlanUsageService planUsageService;

	public InvoiceController(InvoiceService service, PaymentService paymentService, PlanUsageService planUsageService) {
		this.service = service;
		this.paymentService = paymentService;
		this.planUsageService = planUsageService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<InvoiceResponse>> create(
			@Valid @RequestBody InvoiceRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Invoice created", service.create(request, actorId)));
	}

	@PostMapping("/documents")
	public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadDocument(
			@RequestParam("file") MultipartFile file, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Document uploaded", service.uploadDocument(file, actorId)));
	}

	@PostMapping("/import")
	public ResponseEntity<ApiResponse<InvoiceImportResponse>> bulkImport(
			@Valid @RequestBody InvoiceImportRequest request, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		return ResponseEntity.ok(ApiResponse.success("Import terminé", service.bulkImport(request, actorId)));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<InvoiceResponse>>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return Paging.respond("Invoices", service.listPage(Paging.of(page, size)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Invoice", service.get(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<InvoiceResponse>> update(
			@PathVariable String id, @Valid @RequestBody InvoiceRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Invoice updated", service.update(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
		service.delete(id);
		return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
	}

	@PostMapping("/{id}/duplicate")
	public ResponseEntity<ApiResponse<InvoiceResponse>> duplicate(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Invoice duplicated", service.duplicate(id)));
	}

	@PostMapping("/{id}/convert")
	public ResponseEntity<ApiResponse<InvoiceResponse>> convert(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Invoice converted", service.convert(id)));
	}

	@GetMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<List<PaymentResponse>>> payments(@PathVariable String id) {
		return ResponseEntity.ok(ApiResponse.success("Payments", paymentService.listForInvoice(id)));
	}

	@GetMapping("/{id}/pdf")
	public ResponseEntity<byte[]> pdf(@PathVariable String id) {
		byte[] pdf = service.renderPdf(id);
		InvoiceResponse invoice = service.get(id);
		String fileName = (invoice.getStatus() + "-" + invoice.getNumber() + "-" + invoice.getYear() + ".pdf").replace(" ", "-");
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.body(pdf);
	}

	@PostMapping("/{id}/send")
	public ResponseEntity<ApiResponse<Void>> send(
			@PathVariable String id, @RequestBody(required = false) SendInvoiceRequest request) {
		service.sendByEmail(id, request != null ? request.getEmail() : null);
		return ResponseEntity.ok(ApiResponse.success("Invoice e-mailed", null));
	}

	@GetMapping("/export/summary")
	public ResponseEntity<byte[]> exportSummary(@RequestParam(required = false) List<String> ids) {
		byte[] pdf = service.renderSummaryPdf(ids, "Récapitulatif des factures");
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recapitulatif-factures.pdf\"")
				.body(pdf);
	}

	@GetMapping("/export/zip")
	public ResponseEntity<byte[]> exportZip(@RequestParam(required = false) List<String> ids, Authentication authentication) {
		String actorId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
		planUsageService.assertBulkExportEnabled(actorId);
		byte[] zip = service.renderZip(ids);
		return ResponseEntity.ok()
				.contentType(MediaType.valueOf("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factures.zip\"")
				.body(zip);
	}
}
