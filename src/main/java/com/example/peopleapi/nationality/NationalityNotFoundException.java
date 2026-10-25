package com.example.peopleapi.nationality;

public class NationalityNotFoundException extends RuntimeException {

	public NationalityNotFoundException(String name) {
		super("Não foi possível prever a nacionalidade para o nome '" + name + "'");
	}

}
