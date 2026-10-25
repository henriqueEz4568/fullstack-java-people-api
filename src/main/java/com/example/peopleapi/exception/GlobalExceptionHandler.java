package com.example.peopleapi.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.peopleapi.nationality.NationalityNotFoundException;
import com.example.peopleapi.nationality.NationalizeApiException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}
		return validationProblem(errors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ProblemDetail handleInvalidParameter(HandlerMethodValidationException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors()
				.forEach(error -> errors.putIfAbsent(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())));
		return validationProblem(errors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return validationProblem(Map.of(ex.getName(), "valor inválido: '" + ex.getValue() + "'"));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableBody() {
		return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Corpo da requisição ausente ou com JSON inválido");
	}

	@ExceptionHandler(PersonNotFoundException.class)
	ProblemDetail handlePersonNotFound(PersonNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Pessoa não encontrada", ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDuplicatePerson() {
		return problem(HttpStatus.CONFLICT, "Pessoa já cadastrada", "Já existe uma pessoa cadastrada com este documento");
	}

	@ExceptionHandler(NationalityNotFoundException.class)
	ProblemDetail handleNationalityNotFound(NationalityNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Nacionalidade não encontrada", ex.getMessage());
	}

	@ExceptionHandler(NationalizeApiException.class)
	ProblemDetail handleNationalizeApiFailure(NationalizeApiException ex) {
		return problem(HttpStatus.BAD_GATEWAY, "Serviço externo indisponível", ex.getMessage());
	}

	private ProblemDetail validationProblem(Map<String, String> errors) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Um ou mais campos são inválidos");
		problem.setProperty("errors", errors);
		return problem;
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
