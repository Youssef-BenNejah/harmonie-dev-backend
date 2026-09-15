package com.harmoniedev.api.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A monetary amount tagged with its currency code — used wherever a total must not silently mix currencies. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyAmount {
	private String currency;
	private double amount;
}
