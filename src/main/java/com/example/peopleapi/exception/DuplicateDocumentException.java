package com.example.peopleapi.exception;

public class DuplicateDocumentException extends RuntimeException {

	public static final String MESSAGE = "Já existe uma pessoa cadastrada com este documento";

	public DuplicateDocumentException() {
		super(MESSAGE);
	}

}
