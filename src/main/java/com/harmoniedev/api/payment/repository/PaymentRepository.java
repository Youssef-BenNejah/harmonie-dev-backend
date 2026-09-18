package com.harmoniedev.api.payment.repository;

import com.harmoniedev.api.payment.domain.model.PaymentDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<PaymentDocument, String> {
	List<PaymentDocument> findByInvoiceId(String invoiceId);

	List<PaymentDocument> findByCreatedBy(String createdBy);

	Page<PaymentDocument> findByCreatedBy(String createdBy, Pageable pageable);
}
