package com.harmoniedev.api.currency.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
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
	private String code;

	@NotBlank
	private String name;

	@NotBlank
	private String symbol;
}
