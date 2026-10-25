package com.example.peopleapi.repository;

public record PersonFilter(String document, String firstName, String lastName, String email) {

	public static PersonFilter where() {
		return new PersonFilter(null, null, null, null);
	}

	public PersonFilter withDocument(String document) {
		return new PersonFilter(document, firstName, lastName, email);
	}

	public PersonFilter withFirstName(String firstName) {
		return new PersonFilter(document, firstName, lastName, email);
	}

	public PersonFilter withLastName(String lastName) {
		return new PersonFilter(document, firstName, lastName, email);
	}

	public PersonFilter withEmail(String email) {
		return new PersonFilter(document, firstName, lastName, email);
	}

}
