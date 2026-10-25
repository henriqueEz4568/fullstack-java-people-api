package com.example.peopleapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.peopleapi.entity.Person;

@Repository
@Transactional(readOnly = true)
public class PersonRepositoryImpl implements PersonRepository {

	private static final Sort BY_ID = Sort.by("id");

	private final SpringDataPersonRepository jpa;

	PersonRepositoryImpl(SpringDataPersonRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public List<Person> getAll() {
		return jpa.findAll(BY_ID);
	}

	@Override
	public Optional<Person> getOne(PersonFilter where) {
		Person probe = new Person(where.document(), where.firstName(), where.lastName(), where.email());
		return jpa.findBy(Example.of(probe), query -> query.sortBy(BY_ID).first());
	}

	@Override
	@Transactional
	public boolean delete(Long id) {
		if (!jpa.existsById(id)) {
			return false;
		}
		jpa.deleteById(id);
		return true;
	}

	@Override
	@Transactional
	public Person create(Person person) {
		return jpa.saveAndFlush(person);
	}

}
