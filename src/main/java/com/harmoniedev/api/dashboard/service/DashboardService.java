package com.harmoniedev.api.dashboard.service;

import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.client.service.ClientService;
import com.harmoniedev.api.common.dto.CurrencyAmount;
import com.harmoniedev.api.dashboard.domain.dto.response.DashboardSummaryResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.DistributionPointResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.RecentActivityResponse;
import com.harmoniedev.api.dashboard.domain.dto.response.RevenueSeriesPointResponse;
import com.harmoniedev.api.expense.domain.dto.response.DepenseResponse;
import com.harmoniedev.api.expense.service.DepenseService;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.harmoniedev.api.invoice.service.InvoiceService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Every figure here is money, and invoices/expenses may be recorded in different currencies —
 * summing raw numbers across currencies would silently produce a meaningless total. So the
 * headline KPIs (revenue, monthlyExpenses, their trends) are computed for the platform's
 * default currency (TND) only, and the *ByCurrency breakdown fields carry the full picture.
 */
@Service
public class DashboardService {
	private static final String DEFAULT_CURRENCY = "TND";
	private static final String[] MONTH_LABELS =
			{"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

	private final InvoiceService invoiceService;
	private final ClientService clientService;
	private final DepenseService depenseService;

	public DashboardService(InvoiceService invoiceService, ClientService clientService, DepenseService depenseService) {
		this.invoiceService = invoiceService;
		this.clientService = clientService;
		this.depenseService = depenseService;
	}

	public DashboardSummaryResponse summary(String currency) {
		String selected = resolveCurrency(currency);
		List<InvoiceResponse> standardInvoices = invoiceService.list().stream()
				.filter(inv -> "Standard".equals(inv.getType()))
				.toList();
		List<ClientResponse> clients = clientService.list();
		List<DepenseResponse> depenses = depenseService.list();

		double revenue = standardInvoices.stream()
				.filter(inv -> isCurrency(inv, selected))
				.mapToDouble(InvoiceResponse::getPaidAmount)
				.sum();
		long unpaidCount = standardInvoices.stream().filter(inv -> !"Payé".equals(inv.getPaymentStatus())).count();
		YearMonth currentMonth = YearMonth.now();
		double monthlyExpenses = depenses.stream()
				.filter(d -> selected.equals(d.getCurrency()))
				.filter(d -> toYearMonth(d.getCreated()).equals(currentMonth))
				.mapToDouble(DepenseResponse::getPrice)
				.sum();

		List<YearMonth> lastMonths = lastMonths(6);
		List<Double> revenueTrend = lastMonths.stream()
				.map(ym -> standardInvoices.stream()
						.filter(inv -> isCurrency(inv, selected))
						.filter(inv -> toYearMonth(inv.getDate()).equals(ym))
						.mapToDouble(InvoiceResponse::getPaidAmount)
						.sum())
				.toList();
		List<Long> unpaidTrend = lastMonths.stream()
				.map(ym -> standardInvoices.stream()
						.filter(inv -> toYearMonth(inv.getDate()).equals(ym) && !"Payé".equals(inv.getPaymentStatus()))
						.count())
				.toList();
		List<Long> clientsTrend = lastMonths.stream()
				.map(ym -> clients.stream()
						.filter(c -> !toYearMonth(c.getCreated()).isAfter(ym))
						.count())
				.toList();
		List<Double> expensesTrend = lastMonths.stream()
				.map(ym -> depenses.stream()
						.filter(d -> selected.equals(d.getCurrency()))
						.filter(d -> toYearMonth(d.getCreated()).equals(ym))
						.mapToDouble(DepenseResponse::getPrice)
						.sum())
				.toList();

		Map<String, Double> revenueByCurrency = standardInvoices.stream()
				.collect(Collectors.groupingBy(
						inv -> inv.getCurrency() != null ? inv.getCurrency().getCode() : DEFAULT_CURRENCY,
						LinkedHashMap::new,
						Collectors.summingDouble(InvoiceResponse::getPaidAmount)));
		Map<String, Double> expensesByCurrency = depenses.stream()
				.collect(Collectors.groupingBy(DepenseResponse::getCurrency, LinkedHashMap::new, Collectors.summingDouble(DepenseResponse::getPrice)));

		return DashboardSummaryResponse.builder()
				.revenue(round2(revenue))
				.unpaidInvoicesCount(unpaidCount)
				.clientsCount(clients.size())
				.monthlyExpenses(round2(monthlyExpenses))
				.revenueTrend(revenueTrend)
				.unpaidTrend(unpaidTrend)
				.clientsTrend(clientsTrend)
				.expensesTrend(expensesTrend)
				.revenueByCurrency(toSortedCurrencyAmounts(revenueByCurrency, selected))
				.expensesByCurrency(toSortedCurrencyAmounts(expensesByCurrency, selected))
				.build();
	}

	public List<RevenueSeriesPointResponse> revenueSeries(int months, String currency) {
		String selected = resolveCurrency(currency);
		List<InvoiceResponse> standardInvoices = invoiceService.list().stream()
				.filter(inv -> "Standard".equals(inv.getType()))
				.toList();
		List<DepenseResponse> depenses = depenseService.list();

		return lastMonths(months).stream()
				.map(ym -> {
					double revenus = standardInvoices.stream()
							.filter(inv -> isCurrency(inv, selected))
							.filter(inv -> toYearMonth(inv.getDate()).equals(ym))
							.mapToDouble(InvoiceResponse::getTotal)
							.sum();
					double dep = depenses.stream()
							.filter(d -> selected.equals(d.getCurrency()))
							.filter(d -> toYearMonth(d.getCreated()).equals(ym))
							.mapToDouble(DepenseResponse::getPrice)
							.sum();
					return RevenueSeriesPointResponse.builder()
							.mois(monthLabel(ym))
							.revenus(round2(revenus))
							.depenses(round2(dep))
							.build();
				})
				.toList();
	}

	public List<DistributionPointResponse> invoiceStatusDistribution() {
		LocalDate cutoff = LocalDate.now().minusDays(90);
		List<InvoiceResponse> recent = invoiceService.list().stream()
				.filter(inv -> "Standard".equals(inv.getType()))
				.filter(inv -> {
					try {
						return !LocalDate.parse(inv.getDate()).isBefore(cutoff);
					} catch (Exception ex) {
						return false;
					}
				})
				.toList();
		if (recent.isEmpty()) return List.of();

		Map<String, Long> counts = recent.stream()
				.collect(Collectors.groupingBy(InvoiceResponse::getPaymentStatus, Collectors.counting()));
		double total = recent.size();
		return counts.entrySet().stream()
				.map(e -> DistributionPointResponse.builder()
						.name(e.getKey())
						.value(Math.round((e.getValue() / total) * 1000.0) / 10.0)
						.build())
				.sorted(Comparator.comparing(DistributionPointResponse::getValue).reversed())
				.toList();
	}

	public List<InvoiceResponse> recentInvoices(int limit) {
		return invoiceService.list().stream()
				.sorted(Comparator.comparing(InvoiceResponse::getCreated, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(limit)
				.toList();
	}

	public List<RecentActivityResponse> recentActivity(int limit) {
		List<RecentActivityResponse> feed = new ArrayList<>();

		for (InvoiceResponse inv : invoiceService.list()) {
			String clientName = inv.getClient() != null ? clientDisplayName(inv.getClient()) : "";
			feed.add(RecentActivityResponse.builder()
					.id("inv-" + inv.getId())
					.titre(inv.getStatus() + " " + inv.getNumber() + "/" + inv.getYear() + " créée")
					.detail(clientName)
					.temps(inv.getCreated())
					.build());
		}
		for (ClientResponse c : clientService.list()) {
			feed.add(RecentActivityResponse.builder()
					.id("client-" + c.getId())
					.titre("Nouveau client ajouté")
					.detail(clientDisplayName(c))
					.temps(c.getCreated())
					.build());
		}
		for (DepenseResponse d : depenseService.list()) {
			feed.add(RecentActivityResponse.builder()
					.id("dep-" + d.getId())
					.titre("Dépense ajoutée")
					.detail(d.getName())
					.temps(d.getCreated())
					.build());
		}

		return feed.stream()
				.filter(a -> a.getTemps() != null)
				.sorted(Comparator.comparing(RecentActivityResponse::getTemps).reversed())
				.limit(limit)
				.toList();
	}

	private static String clientDisplayName(ClientResponse c) {
		if (c.getPerson() != null) {
			return (c.getPerson().getPrenom() + " " + c.getPerson().getNom()).trim();
		}
		if (c.getEntreprise() != null) {
			return c.getEntreprise().getNom();
		}
		return "";
	}

	private static List<YearMonth> lastMonths(int count) {
		YearMonth current = YearMonth.now();
		List<YearMonth> months = new ArrayList<>();
		for (int i = count - 1; i >= 0; i--) {
			months.add(current.minusMonths(i));
		}
		return months;
	}

	private static YearMonth toYearMonth(String isoDate) {
		try {
			return YearMonth.from(LocalDate.parse(isoDate));
		} catch (Exception ex) {
			return YearMonth.now();
		}
	}

	private static YearMonth toYearMonth(Instant instant) {
		if (instant == null) return YearMonth.now();
		return YearMonth.from(instant.atZone(ZoneOffset.UTC).toLocalDate());
	}

	private static String monthLabel(YearMonth ym) {
		return MONTH_LABELS[ym.getMonthValue() - 1];
	}

	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private static boolean isCurrency(InvoiceResponse invoice, String currency) {
		return invoice.getCurrency() != null && currency.equals(invoice.getCurrency().getCode());
	}

	private static String resolveCurrency(String currency) {
		return (currency == null || currency.isBlank()) ? DEFAULT_CURRENCY : currency;
	}

	/** Selected currency first, then the rest alphabetically — so the UI can always show it up front. */
	private static List<CurrencyAmount> toSortedCurrencyAmounts(Map<String, Double> totals, String selected) {
		return totals.entrySet().stream()
				.map(e -> CurrencyAmount.builder().currency(e.getKey()).amount(round2(e.getValue())).build())
				.sorted(Comparator.comparing((CurrencyAmount c) -> !selected.equals(c.getCurrency()))
						.thenComparing(CurrencyAmount::getCurrency))
				.toList();
	}
}
