package com.harmoniedev.api.expense.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "depenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepenseDocument {
	@Id
	private String id;
	private String name;
	private String categoryId;
	private String currency;
	private double price;
	private String description;
	private String reference;
	private String createdBy;
	@CreatedDate
	private Instant created;
}
