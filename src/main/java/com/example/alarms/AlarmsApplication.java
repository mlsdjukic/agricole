package com.example.alarms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@SpringBootApplication
public class AlarmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlarmsApplication.class, args);
	}

}
