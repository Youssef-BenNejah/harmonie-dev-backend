package com.harmoniedev.api.client.domain.dto.request;

import com.harmoniedev.api.client.domain.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientRequest {
	private ClientType type;
	private String personId;
	private String entrepriseId;
}
