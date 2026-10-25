package com.example.peopleapi.nationality;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.peopleapi.nationality.NationalizeResponse.CountryProbability;

@Service
public class NationalityService {

	private static final Locale PT_BR = Locale.of("pt", "BR");

	private final NationalizeClient client;

	public NationalityService(NationalizeClient client) {
		this.client = client;
	}

	public Nationality predict(String name) {
		List<CountryProbability> countries = Optional.ofNullable(client.predict(name))
				.map(NationalizeResponse::country)
				.orElse(List.of());

		return countries.stream()
				.max(Comparator.comparingDouble(CountryProbability::probability))
				.map(country -> new Nationality(country.countryId(), toCountryName(country.countryId()),
						country.probability()))
				.orElseThrow(() -> new NationalityNotFoundException(name));
	}

	private String toCountryName(String isoCode) {
		return Locale.of("", isoCode).getDisplayCountry(PT_BR);
	}

}
