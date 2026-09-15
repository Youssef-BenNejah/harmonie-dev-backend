package com.harmoniedev.api.payment.service;

import com.harmoniedev.api.invoice.domain.dto.response.InvoiceResponse;
import com.harmoniedev.api.invoice.service.InvoiceService;
import com.harmoniedev.api.payment.domain.dto.request.PaymentRequest;
import com.harmoniedev.api.payment.domain.dto.response.PaymentResponse;
import com.harmoniedev.api.payment.domain.model.PaymentDocument;
import com.harmoniedev.api.payment.repository.PaymentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {
	private final PaymentRepository repository;
	private final InvoiceService invoiceService;

	public PaymentService(PaymentRepository repository, InvoiceService invoiceService) {
		this.repository = repository;
		this.invoiceService = invoiceService;
	}

	public PaymentResponse create(PaymentRequest request, String actorId) {
		InvoiceResponse invoice = invoiceService.get(request.getInvoiceId());
		if (request.getAmountPaid() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
		}
		double newPaidAmount = invoice.getPaidAmount() + request.getAmountPaid();
		if (newPaidAmount > invoice.getTotal() + 0.01) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds the invoice's remaining balance");
		}
		PaymentDocument doc = PaymentDocument.builder()
				.invoiceId(request.getInvoiceId())
				.amountPaid(request.getAmountPaid())
				.paymentMethod(request.getPaymentMethod())
				.paymentDate(request.getPaymentDate())
				.createdBy(actorId)
				.build();
		repository.save(doc);
		invoiceService.applyPayment(request.getInvoiceId(), newPaidAmount);
		return toResponse(doc, invoice);
	}

	public List<PaymentResponse> list() {
		return repository.findAll().stream()
				.map(doc -> toResponse(doc, invoiceService.get(doc.getInvoiceId())))
				.toList();
	}

	public List<PaymentResponse> listForInvoice(String invoiceId) {
		InvoiceResponse invoice = invoiceService.get(invoiceId);
		return repository.findByInvoiceId(invoiceId).stream()
				.map(doc -> toResponse(doc, invoice))
				.toList();
	}

	public PaymentResponse get(String id) {
		PaymentDocument doc = findOrThrow(id);
		return toResponse(doc, invoiceService.get(doc.getInvoiceId()));
	}

	public void delete(String id) {
		PaymentDocument doc = findOrThrow(id);
		InvoiceResponse invoice = invoiceService.get(doc.getInvoiceId());
		double newPaidAmount = Math.max(0, invoice.getPaidAmount() - doc.getAmountPaid());
		repository.deleteById(id);
		invoiceService.applyPayment(doc.getInvoiceId(), newPaidAmount);
	}

	private PaymentDocument findOrThrow(String id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
	}

	private PaymentResponse toResponse(PaymentDocument doc, InvoiceResponse invoice) {
		return PaymentResponse.builder()
				.id(doc.getId())
				.invoice(PaymentResponse.InvoiceSnapshotResponse.builder()
						.id(invoice.getId())
						.number(invoice.getNumber())
						.year(invoice.getYear())
						.type(invoice.getType())
						.total(invoice.getTotal())
						.currency(invoice.getCurrency())
						.build())
				.amountPaid(doc.getAmountPaid())
				.paymentMethod(doc.getPaymentMethod())
				.paymentDate(doc.getPaymentDate())
				.createdBy(doc.getCreatedBy())
				.build();
	}
}
