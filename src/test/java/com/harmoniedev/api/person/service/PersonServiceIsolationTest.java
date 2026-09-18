package com.harmoniedev.api.person.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.harmoniedev.api.auth.domain.enums.Role;
import com.harmoniedev.api.person.domain.model.PersonDocument;
import com.harmoniedev.api.person.repository.PersonRepository;
import com.harmoniedev.api.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PersonServiceIsolationTest {
	@Mock
	private PersonRepository repository;
	private PersonService service;

	@BeforeEach
	void setUp() {
		service = new PersonService(repository);
		AuthenticatedUser caller = new AuthenticatedUser("tenant-b", "b@t.io", Role.USER);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(caller, null, List.of()));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private PersonDocument ownedBy(String owner) {
		return PersonDocument.builder().id("p1").prenom("A").nom("B").createdBy(owner).build();
	}

	@Test
	void get_otherTenantsRecord_isNotFound() {
		when(repository.findById("p1")).thenReturn(Optional.of(ownedBy("tenant-a")));
		assertThatThrownBy(() -> service.get("p1")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
	}

	@Test
	void delete_otherTenantsRecord_isNotFoundAndNothingDeleted() {
		when(repository.findById("p1")).thenReturn(Optional.of(ownedBy("tenant-a")));
		assertThatThrownBy(() -> service.delete("p1")).isInstanceOf(ResponseStatusException.class);
		verify(repository, never()).delete(org.mockito.ArgumentMatchers.any(PersonDocument.class));
		verify(repository, never()).deleteById(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void list_asksOnlyForTheCallersRecords() {
		when(repository.findByCreatedBy("tenant-b")).thenReturn(List.of(ownedBy("tenant-b")));
		assertThat(service.list()).hasSize(1);
		verify(repository, never()).findAll();
	}

	@Test
	void get_ownRecord_succeeds() {
		when(repository.findById("p1")).thenReturn(Optional.of(ownedBy("tenant-b")));
		assertThat(service.get("p1").getId()).isEqualTo("p1");
	}
}
