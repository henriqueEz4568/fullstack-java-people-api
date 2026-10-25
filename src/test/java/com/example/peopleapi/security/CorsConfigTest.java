package com.example.peopleapi.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.peopleapi.controller.PersonController;
import com.example.peopleapi.nationality.NationalityService;
import com.example.peopleapi.repository.PersonRepository;

@WebMvcTest(PersonController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173, https://*.example.com")
class CorsConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PersonRepository repository;

	@MockitoBean
	private NationalityService nationalityService;

	@Test
	void shouldAllowRequestFromConfiguredOrigin() throws Exception {
		mockMvc.perform(get("/list").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void shouldAllowOriginMatchingWildcardPattern() throws Exception {
		mockMvc.perform(get("/list").header(HttpHeaders.ORIGIN, "https://app.example.com"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"));
	}

	@Test
	void shouldAnswerPreflightForDeleteWithApiKeyHeader() throws Exception {
		mockMvc.perform(options("/list/1")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, ApiKeyAuthenticationFilter.HEADER))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,DELETE,OPTIONS"));
	}

	@Test
	void shouldRejectRequestFromOriginNotConfigured() throws Exception {
		mockMvc.perform(get("/list").header(HttpHeaders.ORIGIN, "http://evil.com"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
