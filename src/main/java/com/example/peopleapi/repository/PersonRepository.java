package com.example.peopleapi.repository;

import java.util.List;
import java.util.Optional;

import com.example.peopleapi.entity.Person;

public interface PersonRepository {

	List<Person> getAll();

	Optional<Person> getOne(PersonFilter where);

	boolean delete(Long id);

	Person create(Person person);

}
