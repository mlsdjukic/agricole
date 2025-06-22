package com.example.alarms;

import com.example.alarms.components.Coordinator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing

public class AlarmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlarmsApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(Coordinator coordinator) {
		return args -> coordinator.start();
	}
}
