package com.example.peopleapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {

	@PostMapping("/registrarName")
	public ResponseEntity<Void> register() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@GetMapping("/list")
	public ResponseEntity<Void> list() {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@GetMapping("/list/{document}")
	public ResponseEntity<Void> findByDocument(@PathVariable String document) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@DeleteMapping("/list/{id}")
	public ResponseEntity<Void> deleteById(@PathVariable Long id) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	@GetMapping("/findNacionalityByPerson/{document}")
	public ResponseEntity<Void> findNationalityByPerson(@PathVariable String document) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

}
