package com.harmoniedev.api.currency.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CurrencyRequest {
	@NotBlank
	@Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must be a 3-letter ISO 4217 code (e.g. TND)")
	private String code;

	@NotBlank
	private String name;

	@NotBlank
	private String symbol;
}
