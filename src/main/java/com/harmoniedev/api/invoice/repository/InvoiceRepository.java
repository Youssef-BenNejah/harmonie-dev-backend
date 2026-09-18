package com.harmoniedev.api.invoice.repository;

import com.harmoniedev.api.invoice.domain.model.InvoiceDocument;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvoiceRepository extends MongoRepository<InvoiceDocument, String> {
	List<InvoiceDocument> findByCreatedBy(String createdBy);

	List<InvoiceDocument> findByCreatedByAndTypeAndYear(String createdBy, String type, int year);
}
