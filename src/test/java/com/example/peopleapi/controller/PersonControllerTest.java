package com.example.peopleapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.peopleapi.entity.Person;
import com.example.peopleapi.nationality.NationalityService;
import com.example.peopleapi.repository.PersonFilter;
import com.example.peopleapi.repository.PersonRepository;
import com.example.peopleapi.security.SecurityConfig;

@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
class PersonControllerTest {

	private static final String BODY = """
			{"document": "12345678901", "firstName": "Ana", "lastName": "Silva", "email": "ana@email.com"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PersonRepository repository;

	@MockitoBean
	private NationalityService nationalityService;

	@Test
	void shouldCheckDocumentBeforeInsertingAndNotCreateWhenItExists() throws Exception {
		when(repository.getOne(PersonFilter.where().withDocument("12345678901")))
				.thenReturn(Optional.of(new Person("12345678901", "Ana", "Silva", "ana@email.com")));

		mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe uma pessoa cadastrada com este documento"));

		verify(repository, never()).create(any());
	}

	@Test
	void shouldReturn409WhenDatabaseRejectsDuplicateInsertedConcurrently() throws Exception {
		when(repository.getOne(any())).thenReturn(Optional.empty());
		when(repository.create(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

		mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe uma pessoa cadastrada com este documento"));
	}

}
