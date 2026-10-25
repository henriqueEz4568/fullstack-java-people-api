package com.example.peopleapi.dto;

import com.example.peopleapi.entity.Person;

public record PersonResponse(Long id, String document, String firstName, String lastName, String email) {

	public static PersonResponse from(Person person) {
		return new PersonResponse(person.getId(), person.getDocument(), person.getFirstName(), person.getLastName(),
				person.getEmail());
	}

}
