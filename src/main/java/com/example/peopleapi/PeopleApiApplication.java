package com.example.peopleapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PeopleApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PeopleApiApplication.class, args);
	}

}
