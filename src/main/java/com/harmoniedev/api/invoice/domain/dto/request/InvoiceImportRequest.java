package com.harmoniedev.api.invoice.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceImportRequest {
	/** yyyy-MM-dd — rows dated outside [fromDate, toDate] are rejected. */
	@NotBlank
	private String fromDate;

	@NotBlank
	private String toDate;

	@NotNull
	@NotEmpty
	@Valid
	private List<InvoiceImportRowRequest> rows;
}
