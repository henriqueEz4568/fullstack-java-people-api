package com.example.peopleapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.peopleapi.entity.Person;

interface SpringDataPersonRepository extends JpaRepository<Person, Long> {
}
