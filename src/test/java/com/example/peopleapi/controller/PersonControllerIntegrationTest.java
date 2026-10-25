package com.example.peopleapi.controller;

import static com.example.peopleapi.repository.PersonFilter.where;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import com.example.peopleapi.TestcontainersConfiguration;
import com.example.peopleapi.entity.Person;
import com.example.peopleapi.nationality.NationalizeApiException;
import com.example.peopleapi.nationality.NationalizeClient;
import com.example.peopleapi.nationality.NationalizeResponse;
import com.example.peopleapi.nationality.NationalizeResponse.CountryProbability;
import com.example.peopleapi.repository.PersonRepository;
import com.example.peopleapi.security.ApiKeyAuthenticationFilter;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PersonControllerIntegrationTest {

	private static final String DOCUMENT = "12345678901";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PersonRepository repository;

	@MockitoBean
	private NationalizeClient nationalizeClient;

	@Value("${api.key}")
	private String apiKey;

	@AfterEach
	void cleanUp() {
		repository.getAll().forEach(person -> repository.delete(person.getId()));
	}

	private Person createPerson(String document, String firstName) {
		return repository.create(new Person(document, firstName, "Silva", firstName.toLowerCase() + "@email.com"));
	}

	@Nested
	@DisplayName("POST /registrarName")
	class Register {

		@Test
		void shouldCreatePersonAndReturn201() throws Exception {
			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"document": "12345678901", "firstName": " Ana ", "lastName": "Silva", "email": "ana@email.com"}
									"""))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location", "/list/12345678901"))
					.andExpect(jsonPath("$.id").isNumber())
					.andExpect(jsonPath("$.document").value("12345678901"))
					.andExpect(jsonPath("$.firstName").value("Ana"))
					.andExpect(jsonPath("$.lastName").value("Silva"))
					.andExpect(jsonPath("$.email").value("ana@email.com"));

			assertThat(repository.getOne(where().withDocument(DOCUMENT))).isPresent();
		}

		@Test
		void shouldReturn400WithAllInvalidFields() throws Exception {
			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"document": "123.456.789-01", "firstName": "  ", "email": "not-an-email"}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
					.andExpect(jsonPath("$.title").value("Dados inválidos"))
					.andExpect(jsonPath("$.errors.document").value("deve conter exatamente 11 dígitos numéricos"))
					.andExpect(jsonPath("$.errors.firstName").value("é obrigatório"))
					.andExpect(jsonPath("$.errors.lastName").value("é obrigatório"))
					.andExpect(jsonPath("$.errors.email").value("deve ser um e-mail válido"));

			assertThat(repository.getAll()).isEmpty();
		}

		@Test
		void shouldReturn400WhenDocumentIsMissing() throws Exception {
			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"firstName": "Ana", "lastName": "Silva", "email": "ana@email.com"}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.document").value("é obrigatório"));
		}

		@Test
		void shouldReturn400WhenFieldIsTooLong() throws Exception {
			String longName = "A".repeat(101);

			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"document": "12345678901", "firstName": "%s", "lastName": "Silva", "email": "ana@email.com"}
									""".formatted(longName)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.firstName").value("deve ter no máximo 100 caracteres"));
		}

		@Test
		void shouldReturn400WhenBodyIsMalformed() throws Exception {
			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"document\": "))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou com JSON inválido"));
		}

		@Test
		void shouldReturn400WhenBodyIsMissing() throws Exception {
			mockMvc.perform(post("/registrarName").contentType(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}

		@Test
		void shouldReturn409WhenDocumentAlreadyExists() throws Exception {
			createPerson(DOCUMENT, "Ana");

			mockMvc.perform(post("/registrarName")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"document": "12345678901", "firstName": "Bruno", "lastName": "Souza", "email": "bruno@email.com"}
									"""))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.detail").value("Já existe uma pessoa cadastrada com este documento"));

			assertThat(repository.getAll()).hasSize(1);
		}

	}

	@Nested
	@DisplayName("GET /list")
	class ListAll {

		@Test
		void shouldReturnEmptyList() throws Exception {
			mockMvc.perform(get("/list"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
		}

		@Test
		void shouldReturnAllPeople() throws Exception {
			createPerson("11111111111", "Ana");
			createPerson("22222222222", "Bruno");

			mockMvc.perform(get("/list"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)))
					.andExpect(jsonPath("$[0].document").value("11111111111"))
					.andExpect(jsonPath("$[1].document").value("22222222222"));
		}

	}

	@Nested
	@DisplayName("GET /list/{document}")
	class FindByDocument {

		@Test
		void shouldReturnPerson() throws Exception {
			Person person = createPerson(DOCUMENT, "Ana");

			mockMvc.perform(get("/list/{document}", DOCUMENT))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(person.getId()))
					.andExpect(jsonPath("$.firstName").value("Ana"));
		}

		@Test
		void shouldReturn404WhenPersonDoesNotExist() throws Exception {
			mockMvc.perform(get("/list/{document}", DOCUMENT))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.detail").value("Pessoa com documento '12345678901' não encontrada"));
		}

		@Test
		void shouldReturn400WhenDocumentIsInvalid() throws Exception {
			mockMvc.perform(get("/list/{document}", "abc"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.document").value("deve conter exatamente 11 dígitos numéricos"));
		}

	}

	@Nested
	@DisplayName("DELETE /list/{id}")
	class Delete {

		@Test
		void shouldNotDeletePersonWhenApiKeyIsInvalid() throws Exception {
			Person person = createPerson(DOCUMENT, "Ana");

			mockMvc.perform(delete("/list/{id}", person.getId()).header(ApiKeyAuthenticationFilter.HEADER, "wrong-key"))
					.andExpect(status().isUnauthorized());

			assertThat(repository.getOne(where().withDocument(DOCUMENT))).isPresent();
		}

		@Test
		void shouldDeletePersonWhenApiKeyIsValid() throws Exception {
			Person person = createPerson(DOCUMENT, "Ana");

			mockMvc.perform(delete("/list/{id}", person.getId()).header(ApiKeyAuthenticationFilter.HEADER, apiKey))
					.andExpect(status().isNoContent());

			assertThat(repository.getOne(where().withDocument(DOCUMENT))).isEmpty();
		}

		@Test
		void shouldReturn404WhenPersonDoesNotExist() throws Exception {
			mockMvc.perform(delete("/list/{id}", 999).header(ApiKeyAuthenticationFilter.HEADER, apiKey))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.detail").value("Pessoa com id 999 não encontrada"));
		}

		@Test
		void shouldReturn400WhenIdIsNotNumeric() throws Exception {
			mockMvc.perform(delete("/list/{id}", "abc").header(ApiKeyAuthenticationFilter.HEADER, apiKey))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.id").value("valor inválido: 'abc'"));
		}

		@Test
		void shouldReturn400WhenIdIsNotPositive() throws Exception {
			mockMvc.perform(delete("/list/{id}", -1).header(ApiKeyAuthenticationFilter.HEADER, apiKey))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.id").value("deve ser maior que zero"));
		}

	}

	@Nested
	@DisplayName("GET /findNacionalityByPerson/{document}")
	class FindNationality {

		@Test
		void shouldReturnMostProbableNationality() throws Exception {
			createPerson(DOCUMENT, "Ana");
			when(nationalizeClient.predict("Ana")).thenReturn(new NationalizeResponse("Ana", List.of(
					new CountryProbability("PT", 0.1),
					new CountryProbability("BR", 0.6))));

			mockMvc.perform(get("/findNacionalityByPerson/{document}", DOCUMENT))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.document").value(DOCUMENT))
					.andExpect(jsonPath("$.firstName").value("Ana"))
					.andExpect(jsonPath("$.countryCode").value("BR"))
					.andExpect(jsonPath("$.countryName").value("Brasil"))
					.andExpect(jsonPath("$.probability").value(0.6));
		}

		@Test
		void shouldReturn404WhenPersonDoesNotExist() throws Exception {
			mockMvc.perform(get("/findNacionalityByPerson/{document}", DOCUMENT))
					.andExpect(status().isNotFound());
		}

		@Test
		void shouldReturn404WhenNationalityCannotBePredicted() throws Exception {
			createPerson(DOCUMENT, "Ana");
			when(nationalizeClient.predict("Ana")).thenReturn(new NationalizeResponse("Ana", List.of()));

			mockMvc.perform(get("/findNacionalityByPerson/{document}", DOCUMENT))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.title").value("Nacionalidade não encontrada"));
		}

		@Test
		void shouldReturn502WhenExternalApiFails() throws Exception {
			createPerson(DOCUMENT, "Ana");
			when(nationalizeClient.predict("Ana")).thenThrow(new NationalizeApiException(new RestClientException("timeout")));

			mockMvc.perform(get("/findNacionalityByPerson/{document}", DOCUMENT))
					.andExpect(status().isBadGateway())
					.andExpect(jsonPath("$.detail").value("Falha ao consultar a API de nacionalidade"));
		}

		@Test
		void shouldReturn400WhenDocumentIsInvalid() throws Exception {
			mockMvc.perform(get("/findNacionalityByPerson/{document}", "123"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.errors.document").value("deve conter exatamente 11 dígitos numéricos"));
		}

	}

}
