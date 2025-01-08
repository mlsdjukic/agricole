package com.example.alarms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
public class AlarmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlarmsApplication.class, args);
	}

}
