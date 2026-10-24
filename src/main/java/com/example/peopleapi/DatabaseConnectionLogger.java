package com.example.peopleapi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionLogger implements BeanPostProcessor {

	private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionLogger.class);

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof DataSource dataSource) {
			checkConnection(dataSource);
		}
		return bean;
	}

	private void checkConnection(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			log.info("Database connection established: {} ({} {})",
					metaData.getURL(), metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
		} catch (SQLException e) {
			log.error("Failed to connect to the database: {}", e.getMessage());
			throw new IllegalStateException("Could not connect to the database", e);
		}
	}

}
