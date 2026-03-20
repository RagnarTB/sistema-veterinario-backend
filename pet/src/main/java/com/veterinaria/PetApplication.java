package com.veterinaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // Importa esto

// Le decimos a Spring Boot que NO auto-configure la seguridad por ahora
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class PetApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetApplication.class, args);
	}
}
