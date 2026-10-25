package com.example.peopleapi.nationality;

public class NationalizeApiException extends RuntimeException {

	public NationalizeApiException(Throwable cause) {
		super("Falha ao consultar a API de nacionalidade", cause);
	}

}
