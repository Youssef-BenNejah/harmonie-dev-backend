package com.harmoniedev.api.config;

import com.harmoniedev.api.audit.domain.model.AuditLogDocument;
import com.harmoniedev.api.auth.domain.model.RefreshTokenDocument;
import com.harmoniedev.api.auth.domain.model.UserDocument;
import com.harmoniedev.api.catalog.domain.model.ProductCategoryDocument;
import com.harmoniedev.api.catalog.domain.model.ProductDocument;
import com.harmoniedev.api.client.domain.model.ClientDocument;
import com.harmoniedev.api.company.domain.model.CompanyDocument;
import com.harmoniedev.api.currency.domain.model.CurrencyDocument;
import com.harmoniedev.api.entreprise.domain.model.EntrepriseDocument;
import com.harmoniedev.api.expense.domain.model.DepenseCategoryDocument;
import com.harmoniedev.api.expense.domain.model.DepenseDocument;
import com.harmoniedev.api.invoice.domain.model.InvoiceDocument;
import com.harmoniedev.api.payment.domain.model.PaymentDocument;
import com.harmoniedev.api.person.domain.model.PersonDocument;
import com.harmoniedev.api.tax.domain.model.TaxDocument;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoIndexInitializer {
	private final MongoTemplate mongoTemplate;

	@EventListener(ApplicationReadyEvent.class)
	public void createIndexes() {
		IndexOperations userIndexes = mongoTemplate.indexOps(UserDocument.class);
		userIndexes.createIndex(new Index().on("email", Sort.Direction.ASC).unique().named("users_email_unique"));

		IndexOperations refreshIndexes = mongoTemplate.indexOps(RefreshTokenDocument.class);
		refreshIndexes.createIndex(new Index().on("tokenHash", Sort.Direction.ASC).unique().named("refresh_token_hash_unique"));
		refreshIndexes.createIndex(new Index().on("expiresAt", Sort.Direction.ASC)
				.expire(0L, TimeUnit.SECONDS).named("refresh_token_expires_ttl"));
		refreshIndexes.createIndex(new Index().on("userId", Sort.Direction.ASC).on("revoked", Sort.Direction.ASC)
				.named("refresh_token_user_revoked"));

		for (Class<?> type : List.of(PersonDocument.class, EntrepriseDocument.class, ClientDocument.class,
				TaxDocument.class, CurrencyDocument.class, ProductDocument.class, ProductCategoryDocument.class,
				DepenseDocument.class, DepenseCategoryDocument.class, PaymentDocument.class)) {
			mongoTemplate.indexOps(type).createIndex(new Index().on("createdBy", Sort.Direction.ASC).named("owner_idx"));
		}
		mongoTemplate.indexOps(InvoiceDocument.class).createIndex(new Index()
				.on("createdBy", Sort.Direction.ASC).on("type", Sort.Direction.ASC).on("year", Sort.Direction.ASC)
				.on("number", Sort.Direction.DESC).named("invoice_owner_type_year_number"));
		mongoTemplate.indexOps(PaymentDocument.class).createIndex(new Index().on("invoiceId", Sort.Direction.ASC).named("payment_invoice_idx"));
		mongoTemplate.indexOps(CompanyDocument.class).createIndex(new Index().on("ownerId", Sort.Direction.ASC).named("company_owner_idx"));

		IndexOperations auditIndexes = mongoTemplate.indexOps(AuditLogDocument.class);
		auditIndexes.createIndex(new Index().on("createdAt", Sort.Direction.ASC)
				.expire(7776000L, TimeUnit.SECONDS).named("audit_logs_ttl"));
		auditIndexes.createIndex(new Index().on("userId", Sort.Direction.ASC).on("createdAt", Sort.Direction.ASC)
				.named("audit_logs_user_created"));
	}
}
