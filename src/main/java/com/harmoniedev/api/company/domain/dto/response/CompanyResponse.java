package com.harmoniedev.api.company.domain.dto.response;

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
public class CompanyResponse {
	private String id;
	private String name;
	private String matriculeFisc;
	private String address;
	private String state;
	private String country;
	private String email;
	private String phone;
	private String website;
	private String taxNumber;
	private String vatNumber;
	private String registrationNumber;
	private String logoUrl;
}
