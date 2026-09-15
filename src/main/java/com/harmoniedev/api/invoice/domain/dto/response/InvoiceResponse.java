package com.harmoniedev.api.invoice.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.harmoniedev.api.client.domain.dto.response.ClientResponse;
import com.harmoniedev.api.currency.domain.dto.response.CurrencyResponse;
import java.time.Instant;
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
public class InvoiceResponse {
	private String id;
	private ClientResponse client;
	private int number;
	private int year;
	private CurrencyResponse currency;
	private String status;
	private String paymentStatus;
	private String type;
	@JsonProperty("isConverted")
	private boolean converted;
	private String date;
	private String expirationDate;
	private String note;
	private List<InvoiceItemResponse> items;
	private double timbre;
	private double subtotal;
	private double taxAmount;
	private double total;
	private double paidAmount;
	private String createdBy;
	private Instant created;
	private String factureImage;
}
