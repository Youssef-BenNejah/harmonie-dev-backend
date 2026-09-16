package com.harmoniedev.api.report.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.report.domain.dto.response.ReportOverviewResponse;
import com.harmoniedev.api.report.domain.dto.response.TopClientResponse;
import com.harmoniedev.api.report.domain.dto.response.TopServiceResponse;
import com.harmoniedev.api.report.service.ReportService;
import com.harmoniedev.api.plan.service.PlanUsageService;
import com.harmoniedev.api.security.AuthenticatedUser;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
	private final ReportService service;
	private final PlanUsageService planUsageService;

	public ReportController(ReportService service, PlanUsageService planUsageService) {
		this.service = service;
		this.planUsageService = planUsageService;
	}

	private String actorId(Authentication authentication) {
		return ((AuthenticatedUser) authentication.getPrincipal()).getId();
	}

	@GetMapping("/overview")
	public ResponseEntity<ApiResponse<ReportOverviewResponse>> overview(
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			@RequestParam(required = false) String currency,
			Authentication authentication) {
		planUsageService.assertReportsAccess(actorId(authentication));
		return ResponseEntity.ok(ApiResponse.success("Report overview", service.overview(dateFrom, dateTo, currency)));
	}

	@GetMapping("/top-clients")
	public ResponseEntity<ApiResponse<List<TopClientResponse>>> topClients(
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			@RequestParam(defaultValue = "5") int limit,
			@RequestParam(required = false) String currency,
			Authentication authentication) {
		planUsageService.assertReportsAccess(actorId(authentication));
		return ResponseEntity.ok(ApiResponse.success("Top clients", service.topClients(dateFrom, dateTo, limit, currency)));
	}

	@GetMapping("/top-services")
	public ResponseEntity<ApiResponse<List<TopServiceResponse>>> topServices(
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			@RequestParam(defaultValue = "5") int limit,
			@RequestParam(required = false) String currency,
			Authentication authentication) {
		planUsageService.assertReportsAccess(actorId(authentication));
		return ResponseEntity.ok(ApiResponse.success("Top services", service.topServices(dateFrom, dateTo, limit, currency)));
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(
			@RequestParam(required = false) String dateFrom,
			@RequestParam(required = false) String dateTo,
			@RequestParam(required = false) String currency,
			Authentication authentication) {
		planUsageService.assertReportsAccess(actorId(authentication));
		byte[] pdf = service.exportPdf(dateFrom, dateTo, currency);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport.pdf\"")
				.body(pdf);
	}
}
