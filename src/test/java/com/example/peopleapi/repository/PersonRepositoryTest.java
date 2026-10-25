package com.example.peopleapi.repository;

import static com.example.peopleapi.repository.PersonFilter.where;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.peopleapi.TestcontainersConfiguration;
import com.example.peopleapi.entity.Person;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, PersonRepositoryImpl.class })
class PersonRepositoryTest {

	@Autowired
	private PersonRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Person persist(String document, String firstName, String lastName, String email) {
		return entityManager.persistFlushFind(new Person(document, firstName, lastName, email));
	}

	@Nested
	@DisplayName("getAll")
	class GetAll {

		@Test
		void shouldReturnEmptyListWhenThereAreNoPeople() {
			assertThat(repository.getAll()).isEmpty();
		}

		@Test
		void shouldReturnAllPeopleOrderedById() {
			Person ana = persist("11111111111", "Ana", "Silva", "ana@email.com");
			Person bruno = persist("22222222222", "Bruno", "Souza", "bruno@email.com");

			assertThat(repository.getAll())
					.extracting(Person::getId)
					.containsExactly(ana.getId(), bruno.getId());
		}

	}

	@Nested
	@DisplayName("getOne")
	class GetOne {

		@Test
		void shouldFindPersonByDocument() {
			Person ana = persist("11111111111", "Ana", "Silva", "ana@email.com");
			persist("22222222222", "Bruno", "Souza", "bruno@email.com");

			assertThat(repository.getOne(where().withDocument("11111111111")))
					.get()
					.extracting(Person::getId)
					.isEqualTo(ana.getId());
		}

		@Test
		void shouldMatchAllFilledFields() {
			persist("11111111111", "Ana", "Silva", "ana@email.com");
			Person anaSouza = persist("22222222222", "Ana", "Souza", "ana.souza@email.com");

			assertThat(repository.getOne(where().withFirstName("Ana").withLastName("Souza")))
					.get()
					.extracting(Person::getId)
					.isEqualTo(anaSouza.getId());
		}

		@Test
		void shouldReturnFirstByIdWhenMoreThanOneMatches() {
			Person first = persist("11111111111", "Ana", "Silva", "ana@email.com");
			persist("22222222222", "Ana", "Souza", "ana.souza@email.com");

			assertThat(repository.getOne(where().withFirstName("Ana")))
					.get()
					.extracting(Person::getId)
					.isEqualTo(first.getId());
		}

		@Test
		void shouldReturnEmptyWhenNothingMatches() {
			persist("11111111111", "Ana", "Silva", "ana@email.com");

			assertThat(repository.getOne(where().withEmail("nobody@email.com"))).isEmpty();
		}

	}

	@Nested
	@DisplayName("delete")
	class Delete {

		@Test
		void shouldDeleteExistingPerson() {
			Person ana = persist("11111111111", "Ana", "Silva", "ana@email.com");

			boolean deleted = repository.delete(ana.getId());

			assertThat(deleted).isTrue();
			assertThat(entityManager.find(Person.class, ana.getId())).isNull();
		}

		@Test
		void shouldReturnFalseWhenPersonDoesNotExist() {
			assertThat(repository.delete(999L)).isFalse();
		}

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		void shouldJoinCallerTransactionAndRollBackWithIt() {
			Person ana = repository.create(new Person("11111111111", "Ana", "Silva", "ana@email.com"));
			TransactionTemplate transaction = new TransactionTemplate(transactionManager);

			try {
				transaction.executeWithoutResult(status -> {
					assertThat(repository.delete(ana.getId())).isTrue();
					status.setRollbackOnly();
				});

				assertThat(repository.getOne(where().withDocument("11111111111"))).isPresent();
			} finally {
				repository.delete(ana.getId());
			}
		}

	}

	@Nested
	@DisplayName("create")
	class Create {

		@Test
		void shouldPersistPersonAndGenerateId() {
			Person created = repository.create(new Person("11111111111", "Ana", "Silva", "ana@email.com"));

			assertThat(created.getId()).isNotNull();
			assertThat(entityManager.find(Person.class, created.getId()))
					.extracting(Person::getDocument, Person::getFirstName, Person::getLastName, Person::getEmail)
					.containsExactly("11111111111", "Ana", "Silva", "ana@email.com");
		}

		@Test
		void shouldRejectDuplicateDocument() {
			repository.create(new Person("11111111111", "Ana", "Silva", "ana@email.com"));

			assertThatThrownBy(() -> repository.create(new Person("11111111111", "Bruno", "Souza", "bruno@email.com")))
					.isInstanceOf(DataIntegrityViolationException.class);
		}

	}

}
