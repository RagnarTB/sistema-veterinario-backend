package com.veterinaria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.veterinaria.modelos.Rol;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.RolRespositorio;
import com.veterinaria.respositorios.UsuarioRepositorio;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class PetApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetApplication.class, args);
	}

	@Bean
	CommandLineRunner inicializarDatosBase(
			RolRespositorio rolRepositorio,
			UsuarioRepositorio usuarioRepositorio,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.email:admin@veterinaria.local}") String adminEmail,
			@Value("${app.admin.password:Admin123*}") String adminPassword) {
		return args -> {
			asegurarRol(rolRepositorio, "ROLE_ADMIN");
			asegurarRol(rolRepositorio, "ROLE_CLIENTE");
			asegurarRol(rolRepositorio, "ROLE_RECEPCIONISTA");
			asegurarRol(rolRepositorio, "ROLE_VETERINARIO");

			if (usuarioRepositorio.findByEmail(adminEmail).isPresent()) {
				return;
			}

			Rol rolAdmin = rolRepositorio.findByNombre("ROLE_ADMIN")
					.orElseThrow(() -> new IllegalStateException("El rol ROLE_ADMIN no existe en la BD"));

			Usuario admin = new Usuario();
			admin.setEmail(adminEmail);
			admin.setPassword(passwordEncoder.encode(adminPassword));
			admin.getRoles().add(rolAdmin);

			usuarioRepositorio.save(admin);
			System.out.println("Usuario admin inicializado: " + adminEmail);
		};
	}

	private void asegurarRol(RolRespositorio rolRepositorio, String nombreRol) {
		if (rolRepositorio.findByNombre(nombreRol).isEmpty()) {
			rolRepositorio.save(new Rol(null, nombreRol));
		}
	}
}
