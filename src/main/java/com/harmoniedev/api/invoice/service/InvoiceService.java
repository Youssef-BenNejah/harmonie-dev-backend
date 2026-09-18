package com.harmoniedev.api.invoice.service;

import com.harmoniedev.api.security.TenantScope;
import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.client.domain.model.ClientDocument;
import com.harmoniedev.api.client.repository.ClientRepository;
import com.harmoniedev.api.client.service.ClientService;
import com.harmoniedev.api.company.domain.dto.response.CompanyResponse;
import com.harmoniedev.api.company.service.CompanyService;
import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
import com.harmoniedev.api.currency.service.CurrencyService;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceImportRequest;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceImportRowRequest;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceItemRequest;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceRequest;
import com.harmoniedev.api.invoice.domain.dto.response.DocumentUploadResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceImportResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceImportRowResult;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceItemResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.harmoniedev.api.invoice.domain.model.InvoiceDocument;
import com.harmoniedev.api.invoice.domain.model.InvoiceItemDocument;
import com.harmoniedev.api.invoice.repository.InvoiceRepository;
import com.harmoniedev.api.mail.MailService;
import com.harmoniedev.api.pdf.InvoicePdfService;
import com.harmoniedev.api.plan.service.PlanUsageService;
import com.harmoniedev.api.storage.CloudinaryService;
import com.harmoniedev.api.tax.domain.model.TaxDocument;
import com.harmoniedev.api.tax.repository.TaxRepository;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoiceService {
	private static final Set<String> VALID_PAYMENT_STATUSES = Set.of("impayé", "Partiellement payé", "Payé", "Retard");

	private final InvoiceRepository repository;
	private final ClientService clientService;
	private final ClientRepository clientRepository;
	private final CurrencyService currencyService;
	private final TaxRepository taxRepository;
	private final CompanyService companyService;
	private final InvoicePdfService invoicePdfService;
	private final MailService mailService;
	private final PlanUsageService planUsageService;
	private final CloudinaryService cloudinaryService;

	public InvoiceService(
			InvoiceRepository repository,
			ClientService clientService,
			ClientRepository clientRepository,
			CurrencyService currencyService,
			TaxRepository taxRepository,
			CompanyService companyService,
			InvoicePdfService invoicePdfService,
			MailService mailService,
			PlanUsageService planUsageService,
			CloudinaryService cloudinaryService) {
		this.repository = repository;
		this.clientService = clientService;
		this.clientRepository = clientRepository;
		this.currencyService = currencyService;
		this.taxRepository = taxRepository;
		this.companyService = companyService;
		this.invoicePdfService = invoicePdfService;
		this.mailService = mailService;
		this.planUsageService = planUsageService;
		this.cloudinaryService = cloudinaryService;
	}

	/** Uploads a scanned invoice document (image or PDF) and returns its hosted URL. */
	public DocumentUploadResponse uploadDocument(MultipartFile file, String actorId) {
		var result = cloudinaryService.uploadDocument(file, "invoices/" + actorId, UUID.randomUUID().toString());
		return DocumentUploadResponse.builder().url(result.url()).build();
	}

	public InvoiceResponse create(InvoiceRequest request, String actorId) {
		planUsageService.assertCanCreateInvoice(actorId);
		String type = request.getType() != null && !request.getType().isBlank() ? request.getType() : "Standard";
		int year = Year.parse(request.getDate().substring(0, 4)).getValue();

		clientService.get(request.getClientId());
		currencyService.get(request.getCurrencyId());

		List<InvoiceItemDocument> items = buildItems(request.getItems());
		Totals totals = computeTotals(items, request.getTimbre());

		InvoiceDocument doc = InvoiceDocument.builder()
				.clientId(request.getClientId())
				.currencyId(request.getCurrencyId())
				.number(nextNumber(type, year))
				.year(year)
				.status(request.getStatus())
				.paymentStatus("impayé")
				.type(type)
				.isConverted(false)
				.date(request.getDate())
				.expirationDate(request.getExpirationDate())
				.note(request.getNote())
				.items(items)
				.timbre(request.getTimbre())
				.subtotal(totals.subtotal())
				.taxAmount(totals.taxAmount())
				.total(totals.total())
				.paidAmount(0)
				.createdBy(actorId)
				.factureImage(request.getFactureImage())
				.build();
		repository.save(doc);
		return toResponse(doc);
	}

	/**
	 * Re-enters historical invoices from a previous system (no line-item detail — a single total
	 * per invoice) for a chosen date range. Deliberately bypasses {@link PlanUsageService}: these
	 * are backdated records being migrated in, not new usage the current plan should be judged by.
	 */
	public InvoiceImportResponse bulkImport(InvoiceImportRequest request, String actorId) {
		LocalDate from = LocalDate.parse(request.getFromDate());
		LocalDate to = LocalDate.parse(request.getToDate());
		List<ClientDocument> tenantClients = clientRepository.findByCreatedBy(actorId);
		List<CurrencyResponse> tenantCurrencies = currencyService.list(actorId);

		List<InvoiceImportRowResult> results = new ArrayList<>();
		int imported = 0;
		for (InvoiceImportRowRequest row : request.getRows()) {
			try {
				results.add(importRow(row, from, to, tenantClients, tenantCurrencies, actorId));
				imported++;
			} catch (RowImportException ex) {
				results.add(InvoiceImportRowResult.builder().line(row.getLine()).success(false).message(ex.getMessage()).build());
			}
		}
		return InvoiceImportResponse.builder()
				.imported(imported)
				.failed(results.size() - imported)
				.results(results)
				.build();
	}

	private InvoiceImportRowResult importRow(
			InvoiceImportRowRequest row,
			LocalDate from,
			LocalDate to,
			List<ClientDocument> tenantClients,
			List<CurrencyResponse> tenantCurrencies,
			String actorId) {
		LocalDate date;
		try {
			date = LocalDate.parse(row.getDate());
		} catch (DateTimeParseException ex) {
			throw new RowImportException("Date invalide (attendu AAAA-MM-JJ)");
		}
		if (date.isBefore(from) || date.isAfter(to)) {
			throw new RowImportException("Date hors de la période sélectionnée");
		}

		ClientDocument client = tenantClients.stream()
				.filter(c -> clientDisplayName(c).equalsIgnoreCase(row.getClient().trim()))
				.findFirst()
				.orElseThrow(() -> new RowImportException("Client introuvable : " + row.getClient()));

		CurrencyResponse currency = tenantCurrencies.stream()
				.filter(c -> c.getCode().equalsIgnoreCase(row.getDevise().trim()))
				.findFirst()
				.orElseThrow(() -> new RowImportException("Devise introuvable : " + row.getDevise()));

		String type = row.getType() != null && !row.getType().isBlank() ? row.getType() : "Standard";
		if (!"Standard".equals(type) && !"Proforma".equals(type)) {
			throw new RowImportException("Type invalide : " + row.getType());
		}
		String paymentStatus = row.getStatut() != null && VALID_PAYMENT_STATUSES.contains(row.getStatut())
				? row.getStatut() : "impayé";
		double total = round2(row.getMontant());
		double paid = switch (paymentStatus) {
			case "Payé" -> total;
			case "Partiellement payé" -> row.getMontantPaye() != null ? round2(Math.min(row.getMontantPaye(), total)) : 0;
			default -> 0;
		};
		int year = date.getYear();
		int number = row.getNumero() != null ? row.getNumero() : nextNumber(type, year);

		InvoiceDocument doc = InvoiceDocument.builder()
				.clientId(client.getId())
				.currencyId(currency.getId())
				.number(number)
				.year(year)
				.status("Facture")
				.paymentStatus(paymentStatus)
				.type(type)
				.isConverted(false)
				.date(row.getDate())
				.expirationDate(row.getDate())
				.note(row.getNote())
				.items(List.of())
				.timbre(0)
				.subtotal(total)
				.taxAmount(0)
				.total(total)
				.paidAmount(paid)
				.createdBy(actorId)
				.factureImage(row.getFactureImage())
				.build();
		repository.save(doc);
		return InvoiceImportRowResult.builder().line(row.getLine()).success(true).invoiceId(doc.getId()).build();
	}

	private static String clientDisplayName(ClientDocument client) {
		if (client.getPerson() != null) {
			return (nullToEmpty(client.getPerson().getPrenom()) + " " + nullToEmpty(client.getPerson().getNom())).trim();
		}
		if (client.getEntreprise() != null) return nullToEmpty(client.getEntreprise().getNom());
		return "";
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private static class RowImportException extends RuntimeException {
		RowImportException(String message) {
			super(message);
		}
	}

	public List<InvoiceResponse> list() {
		return repository.findByCreatedBy(TenantScope.currentId()).stream().map(this::toResponse).toList();
	}

	public InvoiceResponse get(String id) {
		return toResponse(findOrThrow(id));
	}

	public InvoiceResponse update(String id, InvoiceRequest request) {
		InvoiceDocument doc = findOrThrow(id);
		clientService.get(request.getClientId());
		currencyService.get(request.getCurrencyId());

		List<InvoiceItemDocument> items = buildItems(request.getItems());
		Totals totals = computeTotals(items, request.getTimbre());

		doc.setClientId(request.getClientId());
		doc.setCurrencyId(request.getCurrencyId());
		doc.setStatus(request.getStatus());
		doc.setDate(request.getDate());
		doc.setExpirationDate(request.getExpirationDate());
		doc.setNote(request.getNote());
		doc.setItems(items);
		doc.setTimbre(request.getTimbre());
		doc.setSubtotal(totals.subtotal());
		doc.setTaxAmount(totals.taxAmount());
		doc.setTotal(totals.total());
		doc.setFactureImage(request.getFactureImage());
		// type is immutable after creation — request.type is ignored here.
		repository.save(doc);
		return toResponse(doc);
	}

	public void delete(String id) {
		repository.delete(findOrThrow(id));
	}

	public InvoiceResponse duplicate(String id) {
		InvoiceDocument source = findOrThrow(id);
		planUsageService.assertCanCreateInvoice(source.getCreatedBy());
		InvoiceDocument copy = InvoiceDocument.builder()
				.clientId(source.getClientId())
				.currencyId(source.getCurrencyId())
				.number(nextNumber(source.getType(), source.getYear()))
				.year(source.getYear())
				.status(source.getStatus())
				.paymentStatus("impayé")
				.type(source.getType())
				.isConverted(false)
				.date(source.getDate())
				.expirationDate(source.getExpirationDate())
				.note(source.getNote())
				.items(source.getItems().stream()
						.map(it -> InvoiceItemDocument.builder()
								.id(UUID.randomUUID().toString())
								.ref(it.getRef())
								.article(it.getArticle())
								.description(it.getDescription())
								.quantity(it.getQuantity())
								.price(it.getPrice())
								.taxId(it.getTaxId())
								.taxRate(it.getTaxRate())
								.taxAmount(it.getTaxAmount())
								.taxName(it.getTaxName())
								.total(it.getTotal())
								.build())
						.toList())
				.timbre(source.getTimbre())
				.subtotal(source.getSubtotal())
				.taxAmount(source.getTaxAmount())
				.total(source.getTotal())
				.paidAmount(0)
				.createdBy(source.getCreatedBy())
				.factureImage(source.getFactureImage())
				.build();
		repository.save(copy);
		return toResponse(copy);
	}

	/** Only valid on a Proforma: marks it converted and creates a Standard duplicate. */
	public InvoiceResponse convert(String id) {
		InvoiceDocument source = findOrThrow(id);
		if (!"Proforma".equals(source.getType())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a Proforma invoice can be converted");
		}
		planUsageService.assertCanCreateInvoice(source.getCreatedBy());
		source.setConverted(true);
		repository.save(source);

		InvoiceDocument standard = InvoiceDocument.builder()
				.clientId(source.getClientId())
				.currencyId(source.getCurrencyId())
				.number(nextNumber("Standard", source.getYear()))
				.year(source.getYear())
				.status(source.getStatus())
				.paymentStatus("impayé")
				.type("Standard")
				.isConverted(false)
				.date(source.getDate())
				.expirationDate(source.getExpirationDate())
				.note(source.getNote())
				.items(source.getItems().stream()
						.map(it -> InvoiceItemDocument.builder()
								.id(UUID.randomUUID().toString())
								.ref(it.getRef())
								.article(it.getArticle())
								.description(it.getDescription())
								.quantity(it.getQuantity())
								.price(it.getPrice())
								.taxId(it.getTaxId())
								.taxRate(it.getTaxRate())
								.taxAmount(it.getTaxAmount())
								.taxName(it.getTaxName())
								.total(it.getTotal())
								.build())
						.toList())
				.timbre(source.getTimbre())
				.subtotal(source.getSubtotal())
				.taxAmount(source.getTaxAmount())
				.total(source.getTotal())
				.paidAmount(0)
				.createdBy(source.getCreatedBy())
				.factureImage(source.getFactureImage())
				.build();
		repository.save(standard);
		return toResponse(standard);
	}

	/** Called by PaymentService after recording/reversing a payment. */
	public void applyPayment(String invoiceId, double newPaidAmount) {
		InvoiceDocument doc = findOrThrow(invoiceId);
		doc.setPaidAmount(newPaidAmount);
		doc.setPaymentStatus(derivePaymentStatus(newPaidAmount, doc.getTotal(), doc.getExpirationDate()));
		repository.save(doc);
	}

	InvoiceDocument findOrThrow(String id) {
		return repository.findById(id)
				.filter(i -> TenantScope.owns(i.getCreatedBy()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
	}

	public byte[] renderPdf(String id) {
		InvoiceResponse invoice = get(id);
		CompanyResponse company = companyService.getOrCreate(findOrThrow(id).getCreatedBy());
		return invoicePdfService.generateInvoicePdf(invoice, company);
	}

	public byte[] renderSummaryPdf(List<String> ids, String title) {
		List<InvoiceResponse> invoices = ids == null || ids.isEmpty()
				? list()
				: ids.stream().map(this::get).toList();
		return invoicePdfService.generateSummaryPdf(invoices, title);
	}

	public List<InvoiceResponse> resolveForExport(List<String> ids) {
		return ids == null || ids.isEmpty() ? list() : ids.stream().map(this::get).toList();
	}

	public byte[] renderZip(List<String> ids) {
		List<InvoiceResponse> invoices = resolveForExport(ids);
		var out = new java.io.ByteArrayOutputStream();
		try (var zip = new java.util.zip.ZipOutputStream(out)) {
			for (InvoiceResponse invoice : invoices) {
				CompanyResponse company = companyService.getOrCreate(findOrThrow(invoice.getId()).getCreatedBy());
				byte[] pdf = invoicePdfService.generateInvoicePdf(invoice, company);
				String label = invoice.getStatus() + " " + invoice.getNumber() + "/" + invoice.getYear();
				String who = clientName(invoice.getClient());
				String fileName = (label + (who.isBlank() ? "" : "-" + who) + ".pdf").replace(" ", "-").replace("/", "-");
				zip.putNextEntry(new java.util.zip.ZipEntry(fileName));
				zip.write(pdf);
				zip.closeEntry();
			}
		} catch (java.io.IOException ex) {
			throw new IllegalStateException("Failed to build ZIP export", ex);
		}
		return out.toByteArray();
	}

	/** Emails the invoice PDF to the client (or an override address). Defaults to the client's own email. */
	public void sendByEmail(String id, String overrideEmail) {
		InvoiceResponse invoice = get(id);
		String toEmail = overrideEmail != null && !overrideEmail.isBlank() ? overrideEmail : clientEmail(invoice.getClient());
		if (toEmail == null || toEmail.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No recipient e-mail available for this client");
		}
		byte[] pdf = renderPdf(id);
		String label = invoice.getStatus() + " " + invoice.getNumber() + "/" + invoice.getYear();
		String fileName = label.replace(" ", "-").replace("/", "-") + ".pdf";
		mailService.sendInvoiceEmail(toEmail, clientName(invoice.getClient()), label, pdf, fileName);
	}

	private static String clientEmail(ClientResponse client) {
		if (client == null) return null;
		if (client.getPerson() != null) return client.getPerson().getEmail();
		if (client.getEntreprise() != null) return client.getEntreprise().getEmail();
		return null;
	}

	private static String clientName(ClientResponse client) {
		if (client == null) return "";
		if (client.getPerson() != null) {
			return (client.getPerson().getPrenom() + " " + client.getPerson().getNom()).trim();
		}
		if (client.getEntreprise() != null) return client.getEntreprise().getNom();
		return "";
	}

	private int nextNumber(String type, int year) {
		return repository.findByCreatedByAndTypeAndYear(TenantScope.currentId(), type, year).stream()
				.mapToInt(InvoiceDocument::getNumber)
				.max()
				.orElse(0) + 1;
	}

	private List<InvoiceItemDocument> buildItems(List<InvoiceItemRequest> requests) {
		return requests.stream().map(item -> {
			double base = item.getQuantity() * item.getPrice();
			TaxDocument tax = (item.getTaxId() != null && !item.getTaxId().isBlank())
					? taxRepository.findById(item.getTaxId()).filter(t -> TenantScope.owns(t.getCreatedBy())).orElse(null) : null;
			double taxRate = tax != null ? tax.getTaxvalue() : 0;
			double taxAmount = round2(base * taxRate / 100);
			return InvoiceItemDocument.builder()
					.id(UUID.randomUUID().toString())
					.ref(item.getRef())
					.article(item.getArticle())
					.description(item.getDescription())
					.quantity(item.getQuantity())
					.price(item.getPrice())
					.taxId(tax != null ? tax.getId() : "")
					.taxRate(taxRate)
					.taxAmount(taxAmount)
					.taxName(tax != null ? tax.getName() : "")
					.total(round2(base + taxAmount))
					.build();
		}).toList();
	}

	private Totals computeTotals(List<InvoiceItemDocument> items, double timbre) {
		double itemsTotal = round2(items.stream().mapToDouble(InvoiceItemDocument::getTotal).sum());
		double taxAmount = round2(items.stream().mapToDouble(InvoiceItemDocument::getTaxAmount).sum());
		double subtotal = round2(itemsTotal + timbre);
		return new Totals(subtotal, taxAmount, subtotal);
	}

	private record Totals(double subtotal, double taxAmount, double total) {
	}

	private static String derivePaymentStatus(double paid, double total, String expirationDate) {
		if (paid >= total && total > 0) return "Payé";
		boolean overdue = false;
		try {
			overdue = expirationDate != null && LocalDate.parse(expirationDate).isBefore(LocalDate.now());
		} catch (Exception ignored) {
			// unparsable/blank due date — treat as not overdue
		}
		if (paid <= 0) {
			return overdue ? "Retard" : "impayé";
		}
		return overdue ? "Retard" : "Partiellement payé";
	}

	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private InvoiceResponse toResponse(InvoiceDocument doc) {
		ClientResponse client;
		CurrencyResponse currency;
		try {
			client = clientService.get(doc.getClientId());
		} catch (ResponseStatusException ex) {
			client = null;
		}
		try {
			currency = currencyService.get(doc.getCurrencyId());
		} catch (ResponseStatusException ex) {
			currency = null;
		}
		return InvoiceResponse.builder()
				.id(doc.getId())
				.client(client)
				.number(doc.getNumber())
				.year(doc.getYear())
				.currency(currency)
				.status(doc.getStatus())
				.paymentStatus(doc.getPaymentStatus())
				.type(doc.getType())
				.converted(doc.isConverted())
				.date(doc.getDate())
				.expirationDate(doc.getExpirationDate())
				.note(doc.getNote())
				.items(doc.getItems().stream()
						.map(it -> InvoiceItemResponse.builder()
								.id(it.getId())
								.ref(it.getRef())
								.article(it.getArticle())
								.description(it.getDescription())
								.quantity(it.getQuantity())
								.price(it.getPrice())
								.taxId(it.getTaxId())
								.taxRate(it.getTaxRate())
								.taxAmount(it.getTaxAmount())
								.taxName(it.getTaxName())
								.total(it.getTotal())
								.build())
						.toList())
				.timbre(doc.getTimbre())
				.subtotal(doc.getSubtotal())
				.taxAmount(doc.getTaxAmount())
				.total(doc.getTotal())
				.paidAmount(doc.getPaidAmount())
				.createdBy(doc.getCreatedBy())
				.created(doc.getCreated())
				.factureImage(doc.getFactureImage())
				.build();
	}
}
