package com.harmoniedev.api.dashboard.controller;

import com.harmoniedev.api.common.dto.ApiResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.DashboardSummaryResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.DistributionPointResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.RecentActivityResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.RevenueSeriesPointResponse;
import com.harmoniedev.api.dashboard.service.DashboardService;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
	private final DashboardService service;

	public DashboardController(DashboardService service) {
		this.service = service;
	}

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
			@RequestParam(required = false) String currency) {
		return ResponseEntity.ok(ApiResponse.success("Dashboard summary", service.summary(currency)));
	}

	@GetMapping("/revenue-series")
	public ResponseEntity<ApiResponse<List<RevenueSeriesPointResponse>>> revenueSeries(
			@RequestParam(defaultValue = "12") int months, @RequestParam(required = false) String currency) {
		return ResponseEntity.ok(ApiResponse.success("Revenue series", service.revenueSeries(months, currency)));
	}

	@GetMapping("/invoice-status-distribution")
	public ResponseEntity<ApiResponse<List<DistributionPointResponse>>> invoiceStatusDistribution() {
		return ResponseEntity.ok(ApiResponse.success("Invoice status distribution", service.invoiceStatusDistribution()));
	}

	@GetMapping("/recent-invoices")
	public ResponseEntity<ApiResponse<List<InvoiceResponse>>> recentInvoices(
			@RequestParam(defaultValue = "5") int limit) {
		return ResponseEntity.ok(ApiResponse.success("Recent invoices", service.recentInvoices(limit)));
	}

	@GetMapping("/recent-activity")
	public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> recentActivity(
			@RequestParam(defaultValue = "5") int limit) {
		return ResponseEntity.ok(ApiResponse.success("Recent activity", service.recentActivity(limit)));
	}
}
