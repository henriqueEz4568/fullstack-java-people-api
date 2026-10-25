package com.example.peopleapi.nationality;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NationalizeClient {

	private final RestClient restClient;

	public NationalizeClient(RestClient.Builder builder, @Value("${nationalize.url}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	public NationalizeResponse predict(String name) {
		try {
			return restClient.get()
					.uri(uri -> uri.path("/").queryParam("name", name).build())
					.retrieve()
					.body(NationalizeResponse.class);
		} catch (RestClientException e) {
			throw new NationalizeApiException(e);
		}
	}

}
