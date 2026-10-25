package com.example.peopleapi.dto;

import com.example.peopleapi.entity.Person;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PersonRequest(
		@NotNull(message = "é obrigatório")
		@Pattern(regexp = PersonRequest.DOCUMENT_PATTERN, message = PersonRequest.DOCUMENT_MESSAGE)
		String document,

		@NotBlank(message = "é obrigatório")
		@Size(max = 100, message = "deve ter no máximo 100 caracteres")
		String firstName,

		@NotBlank(message = "é obrigatório")
		@Size(max = 100, message = "deve ter no máximo 100 caracteres")
		String lastName,

		@NotBlank(message = "é obrigatório")
		@Email(message = "deve ser um e-mail válido")
		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String email) {

	public static final String DOCUMENT_PATTERN = "\\d{11}";

	public static final String DOCUMENT_MESSAGE = "deve conter exatamente 11 dígitos numéricos";

	public Person toEntity() {
		return new Person(document, firstName.trim(), lastName.trim(), email.trim());
	}

}
