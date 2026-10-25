package com.example.peopleapi.exception;

public class PersonNotFoundException extends RuntimeException {

	public PersonNotFoundException(String message) {
		super(message);
	}

	public static PersonNotFoundException byDocument(String document) {
		return new PersonNotFoundException("Pessoa com documento '" + document + "' não encontrada");
	}

	public static PersonNotFoundException byId(Long id) {
		return new PersonNotFoundException("Pessoa com id " + id + " não encontrada");
	}

}
