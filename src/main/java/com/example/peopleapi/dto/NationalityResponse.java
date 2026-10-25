package com.example.peopleapi.dto;

import com.example.peopleapi.entity.Person;
import com.example.peopleapi.nationality.Nationality;

public record NationalityResponse(String document, String firstName, String countryCode, String countryName,
		double probability) {

	public static NationalityResponse of(Person person, Nationality nationality) {
		return new NationalityResponse(person.getDocument(), person.getFirstName(), nationality.countryCode(),
				nationality.countryName(), nationality.probability());
	}

}
