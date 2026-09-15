package com.harmoniedev.api.currency.domain.dto.response;

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
public class CurrencyResponse {
	private String id;
	private String code;
	private String name;
	private String symbol;
	private String createdBy;
}
