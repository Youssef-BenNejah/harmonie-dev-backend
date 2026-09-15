package com.harmoniedev.api.company.domain.dto.request;

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
public class CompanyRequest {
	@NotBlank
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
}
