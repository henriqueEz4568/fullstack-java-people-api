package com.example.peopleapi.nationality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.peopleapi.nationality.NationalizeResponse.CountryProbability;

class NationalityServiceTest {

	private final NationalizeClient client = mock(NationalizeClient.class);

	private final NationalityService service = new NationalityService(client);

	@Test
	void shouldReturnCountryWithHighestProbability() {
		when(client.predict("nathaniel")).thenReturn(new NationalizeResponse("nathaniel", List.of(
				new CountryProbability("PH", 0.08),
				new CountryProbability("NG", 0.05),
				new CountryProbability("US", 0.12))));

		assertThat(service.predict("nathaniel"))
				.isEqualTo(new Nationality("US", "Estados Unidos", 0.12));
	}

	@Test
	void shouldConvertIsoCodeToCountryNameInPortuguese() {
		when(client.predict("hans")).thenReturn(new NationalizeResponse("hans", List.of(new CountryProbability("DE", 0.5))));

		assertThat(service.predict("hans").countryName()).isEqualTo("Alemanha");
	}

	@Test
	void shouldThrowWhenApiReturnsNoCountries() {
		when(client.predict("xyz")).thenReturn(new NationalizeResponse("xyz", List.of()));

		assertThatThrownBy(() -> service.predict("xyz"))
				.isInstanceOf(NationalityNotFoundException.class)
				.hasMessage("Não foi possível prever a nacionalidade para o nome 'xyz'");
	}

	@Test
	void shouldThrowWhenApiReturnsNullCountryList() {
		when(client.predict("xyz")).thenReturn(new NationalizeResponse("xyz", null));

		assertThatThrownBy(() -> service.predict("xyz")).isInstanceOf(NationalityNotFoundException.class);
	}

}
