package com.example.peopleapi.nationality;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NationalizeResponse(String name, List<CountryProbability> country) {

	public record CountryProbability(@JsonProperty("country_id") String countryId, double probability) {
	}

}
