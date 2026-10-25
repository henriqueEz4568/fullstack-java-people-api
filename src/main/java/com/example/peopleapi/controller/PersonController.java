package com.example.peopleapi.controller;

import static com.example.peopleapi.repository.PersonFilter.where;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.peopleapi.dto.NationalityResponse;
import com.example.peopleapi.dto.PersonRequest;
import com.example.peopleapi.dto.PersonResponse;
import com.example.peopleapi.entity.Person;
import com.example.peopleapi.exception.PersonNotFoundException;
import com.example.peopleapi.nationality.NationalityService;
import com.example.peopleapi.repository.PersonRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@RestController
public class PersonController {

	private final PersonRepository repository;

	private final NationalityService nationalityService;

	public PersonController(PersonRepository repository, NationalityService nationalityService) {
		this.repository = repository;
		this.nationalityService = nationalityService;
	}

	@PostMapping("/registrarName")
	public ResponseEntity<PersonResponse> register(@Valid @RequestBody PersonRequest request) {
		Person person = repository.create(request.toEntity());
		return ResponseEntity.created(URI.create("/list/" + person.getDocument())).body(PersonResponse.from(person));
	}

	@GetMapping("/list")
	public List<PersonResponse> list() {
		return repository.getAll().stream().map(PersonResponse::from).toList();
	}

	@GetMapping("/list/{document}")
	public PersonResponse findByDocument(
			@PathVariable @Pattern(regexp = PersonRequest.DOCUMENT_PATTERN, message = PersonRequest.DOCUMENT_MESSAGE) String document) {
		return PersonResponse.from(findPerson(document));
	}

	@DeleteMapping("/list/{id}")
	public ResponseEntity<Void> deleteById(@PathVariable @Positive(message = "deve ser maior que zero") Long id) {
		if (!repository.delete(id)) {
			throw PersonNotFoundException.byId(id);
		}
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/findNacionalityByPerson/{document}")
	public NationalityResponse findNationalityByPerson(
			@PathVariable @Pattern(regexp = PersonRequest.DOCUMENT_PATTERN, message = PersonRequest.DOCUMENT_MESSAGE) String document) {
		Person person = findPerson(document);
		return NationalityResponse.of(person, nationalityService.predict(person.getFirstName()));
	}

	private Person findPerson(String document) {
		return repository.getOne(where().withDocument(document))
				.orElseThrow(() -> PersonNotFoundException.byDocument(document));
	}

}
