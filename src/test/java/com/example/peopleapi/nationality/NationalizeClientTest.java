package com.example.peopleapi.nationality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.example.peopleapi.nationality.NationalizeResponse.CountryProbability;

@RestClientTest(NationalizeClient.class)
class NationalizeClientTest {

	@Autowired
	private NationalizeClient client;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void shouldCallApiWithNameAndParseResponse() {
		server.expect(requestTo("https://api.nationalize.io/?name=nathaniel"))
				.andRespond(withSuccess("""
						{"count": 1000, "name": "nathaniel", "country": [
							{"country_id": "PH", "probability": 0.08},
							{"country_id": "NG", "probability": 0.05}
						]}
						""", MediaType.APPLICATION_JSON));

		NationalizeResponse response = client.predict("nathaniel");

		assertThat(response.name()).isEqualTo("nathaniel");
		assertThat(response.country()).containsExactly(
				new CountryProbability("PH", 0.08),
				new CountryProbability("NG", 0.05));
		server.verify();
	}

	@Test
	void shouldThrowNationalizeApiExceptionWhenApiFails() {
		server.expect(requestTo("https://api.nationalize.io/?name=nathaniel")).andRespond(withServerError());

		assertThatThrownBy(() -> client.predict("nathaniel")).isInstanceOf(NationalizeApiException.class);
	}

}
