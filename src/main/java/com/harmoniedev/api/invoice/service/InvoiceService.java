package com.harmoniedev.api.invoice.service;

import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.client.service.ClientService;
import com.harmoniedev.api.company.domain.dto.response.CompanyResponse;
import com.harmoniedev.api.company.service.CompanyService;
import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
import com.harmoniedev.api.currency.service.CurrencyService;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceItemRequest;
import com.harmoniedev.api.invoice.domain.dto.request.InvoiceRequest;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceItemResponse;
import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.harmoniedev.api.invoice.domain.model.InvoiceDocument;
import com.harmoniedev.api.invoice.domain.model.InvoiceItemDocument;
import com.harmoniedev.api.invoice.repository.InvoiceRepository;
import com.harmoniedev.api.mail.MailService;
import com.harmoniedev.api.pdf.InvoicePdfService;
import com.harmoniedev.api.tax.domain.model.TaxDocument;
import com.harmoniedev.api.tax.repository.TaxRepository;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InvoiceService {
	private final InvoiceRepository repository;
	private final ClientService clientService;
	private final CurrencyService currencyService;
	private final TaxRepository taxRepository;
	private final CompanyService companyService;
	private final InvoicePdfService invoicePdfService;
	private final MailService mailService;

	public InvoiceService(
			InvoiceRepository repository,
			ClientService clientService,
			CurrencyService currencyService,
			TaxRepository taxRepository,
			CompanyService companyService,
			InvoicePdfService invoicePdfService,
			MailService mailService) {
		this.repository = repository;
		this.clientService = clientService;
		this.currencyService = currencyService;
		this.taxRepository = taxRepository;
		this.companyService = companyService;
		this.invoicePdfService = invoicePdfService;
		this.mailService = mailService;
	}

	public InvoiceResponse create(InvoiceRequest request, String actorId) {
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

	public List<InvoiceResponse> list() {
		return repository.findAll().stream().map(this::toResponse).toList();
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
		if (!repository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
		}
		repository.deleteById(id);
	}

	public InvoiceResponse duplicate(String id) {
		InvoiceDocument source = findOrThrow(id);
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
		return repository.findByTypeAndYear(type, year).stream()
				.mapToInt(InvoiceDocument::getNumber)
				.max()
				.orElse(0) + 1;
	}

	private List<InvoiceItemDocument> buildItems(List<InvoiceItemRequest> requests) {
		return requests.stream().map(item -> {
			double base = item.getQuantity() * item.getPrice();
			TaxDocument tax = (item.getTaxId() != null && !item.getTaxId().isBlank())
					? taxRepository.findById(item.getTaxId()).orElse(null) : null;
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
