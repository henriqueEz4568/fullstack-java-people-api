package com.example.peopleapi.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.example.peopleapi.PersonController;

@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "api.key=test-api-key")
class SecurityConfigTest {

	private static final String VALID_KEY = "test-api-key";

	@Autowired
	private MockMvc mockMvc;

	@Nested
	@DisplayName("DELETE /list/{id}")
	class DeleteRoute {

		@Test
		void shouldReturnUnauthorizedWithoutApiKey() throws Exception {
			mockMvc.perform(delete("/list/1"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void shouldReturnUnauthorizedWithInvalidApiKey() throws Exception {
			mockMvc.perform(delete("/list/1").header(ApiKeyAuthenticationFilter.HEADER, "wrong-key"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		void shouldReachControllerWithValidApiKey() throws Exception {
			mockMvc.perform(delete("/list/1").header(ApiKeyAuthenticationFilter.HEADER, VALID_KEY))
					.andExpect(status().isNotImplemented());
		}

	}

	@Nested
	@DisplayName("Public routes")
	class PublicRoutes {

		@Test
		void shouldAllowRegisterWithoutApiKey() throws Exception {
			mockMvc.perform(post("/registrarName"))
					.andExpect(status().isNotImplemented());
		}

		@Test
		void shouldAllowListWithoutApiKey() throws Exception {
			mockMvc.perform(get("/list"))
					.andExpect(status().isNotImplemented());
		}

		@Test
		void shouldAllowFindByDocumentWithoutApiKey() throws Exception {
			mockMvc.perform(get("/list/12345678901"))
					.andExpect(status().isNotImplemented());
		}

		@Test
		void shouldAllowFindNationalityWithoutApiKey() throws Exception {
			mockMvc.perform(get("/findNacionalityByPerson/12345678901"))
					.andExpect(status().isNotImplemented());
		}

	}

}
