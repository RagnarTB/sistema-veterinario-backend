package com.veterinaria;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.veterinaria.modelos.*;
import com.veterinaria.respositorios.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing // al arrancar la app se creara una tabla espejo llamada atenciones_medicas_AUD
					// y registrara los cambios automaticamente
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class PetApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetApplication.class, args);
	}

	// Este código se ejecuta automáticamente una sola vez cuando arranca el
	// servidor
	@Bean
	CommandLineRunner inicializarDatos(RolRespositorio rolRepositorio,
			PermisoRepositorio permisoRepositorio,
			UsuarioRepositorio usuarioRepositorio,
			EmpleadoRepositorio empleadoRepositorio,
			SedeRepositorio sedeRepositorio,
			CategoriaProductoRepositorio categoriaProductoRepositorio,
			UnidadMedidaRepositorio unidadMedidaRepositorio,
			ProveedorRepositorio proveedorRepositorio,
			EspecieRepositorio especieRepositorio,
			ServicioMedicoRepositorio servicioMedicoRepositorio,
			CategoriaJaulaRepositorio categoriaJaulaRepositorio,
			TamanoJaulaRepositorio tamanoJaulaRepositorio,
			JaulaRepositorio jaulaRepositorio,
			ProductoRepositorio productoRepositorio,
			PasswordEncoder passwordEncoder) {
		return args -> {
			// 1. Inicializar Roles
			if (rolRepositorio.count() == 0) {
				rolRepositorio.save(new Rol(null, "ROLE_ADMIN", true));
				rolRepositorio.save(new Rol(null, "ROLE_CLIENTE", true));
				rolRepositorio.save(new Rol(null, "ROLE_RECEPCIONISTA", true));
				rolRepositorio.save(new Rol(null, "ROLE_VETERINARIO", true));
				System.out.println(" Roles inicializados");
			}

			// 1.5 Inicializar Permisos
			if (permisoRepositorio.count() == 0) {
				String[][] permisosBase = {
						{ "VER_CITAS", "Ver listado de citas" }, { "CREAR_CITAS", "Agendar nuevas citas" },
						{ "EDITAR_CITAS", "Editar citas existentes" },
						{ "AVANZAR_ESTADO_CITAS", "Avanzar estados en Kanban de citas" },
						{ "VER_PACIENTES", "Ver listado de pacientes" },
						{ "CREAR_PACIENTES", "Crear nuevos pacientes" },
						{ "EDITAR_PACIENTES", "Editar información de pacientes" },
						{ "VER_CLIENTES", "Ver listado de clientes" },
						{ "GESTIONAR_CLIENTES", "Crear, editar o desactivar clientes" },
						{ "VER_HISTORIAL", "Ver historial clínico" },
						{ "CREAR_REGISTROS_CLINICOS", "Añadir exámenes, vacunas, cirugías, recetas" },
						{ "GESTIONAR_HOSPITALIZACION", "Ingresar y monitorear pacientes en hospitalización" },
						{ "VER_CAJA", "Ver estado de caja y movimientos" },
						{ "ABRIR_CERRAR_CAJA", "Abrir o cerrar turnos de caja" },
						{ "VER_VENTAS", "Ver listado de ventas" }, { "CREAR_VENTAS", "Registrar ventas o pagos" },
						{ "VER_INVENTARIO", "Ver stock e inventario" },
						{ "GESTIONAR_INVENTARIO", "Registrar entradas, salidas o ajustes" },
						{ "GESTIONAR_CATALOGO", "Configurar categorías y productos" },
						{ "VER_FINANZAS", "Ver deudas y resúmenes financieros" },
						{ "VER_REPORTES", "Ver reportes y dashboards" },
						{ "GESTIONAR_EMPLEADOS", "Configurar personal y empleados" },
						{ "GESTIONAR_ROLES_PERMISOS", "Crear roles y modificar permisos" },
						{ "GESTIONAR_CONFIGURACION", "Configurar sedes, jaulas y horarios" }
				};
				for (String[] p : permisosBase) {
					permisoRepositorio.save(new Permiso(p[0], p[1]));
				}
				System.out.println(" Permisos inicializados");

				// Asignar permisos a roles existentes por defecto
				java.util.List<Permiso> todos = permisoRepositorio.findAll();

				Rol admin = rolRepositorio.findByNombre("ROLE_ADMIN").orElse(null);
				if (admin != null) {
					admin.getPermisos().addAll(todos);
					rolRepositorio.save(admin);
				}

				Rol vet = rolRepositorio.findByNombre("ROLE_VETERINARIO").orElse(null);
				if (vet != null) {
					todos.stream().filter(p -> p.getNombre().contains("CITAS") || p.getNombre().contains("PACIENTES")
							|| p.getNombre().contains("HISTORIAL") || p.getNombre().contains("CLINICOS")
							|| p.getNombre().contains("HOSPITALIZACION") || p.getNombre().equals("VER_INVENTARIO"))
							.forEach(vet.getPermisos()::add);
					rolRepositorio.save(vet);
				}

				Rol rec = rolRepositorio.findByNombre("ROLE_RECEPCIONISTA").orElse(null);
				if (rec != null) {
					todos.stream()
							.filter(p -> p.getNombre().contains("CITAS") || p.getNombre().contains("PACIENTES")
									|| p.getNombre().contains("CLIENTES") || p.getNombre().contains("CAJA")
									|| p.getNombre().contains("VENTAS") || p.getNombre().equals("VER_INVENTARIO"))
							.forEach(rec.getPermisos()::add);
					rolRepositorio.save(rec);
				}

				Rol cli = rolRepositorio.findByNombre("ROLE_CLIENTE").orElse(null);
				if (cli != null) {
					todos.stream()
							.filter(p -> p.getNombre().equals("VER_CITAS") || p.getNombre().equals("CREAR_CITAS")
									|| p.getNombre().equals("VER_PACIENTES") || p.getNombre().equals("VER_HISTORIAL"))
							.forEach(cli.getPermisos()::add);
					rolRepositorio.save(cli);
				}
				System.out.println(" Permisos asignados a los roles base");
			}

			// Auditoría de Permisos Granulares (Añadido en Fase de Mejora)
			String[][] nuevosPermisos = {
					{ "GESTIONAR_PACIENTES", "Crear, editar y desactivar pacientes" },
					{ "GESTIONAR_CITAS", "Avanzar estados de cita (recepción)" },
					{ "REGISTRAR_ATENCION", "Guardar fichas clínicas y recetas" },
					{ "REALIZAR_VENTAS", "Usar el POS / Cobrar ventas" },
					{ "GESTIONAR_INVENTARIO", "Crear/editar productos, ingresar/sacar stock" },
					{ "OPERAR_CAJA", "Abrir/cerrar caja, registrar ingresos/egresos" },
					{ "GESTIONAR_FINANZAS", "Registrar pagos de deudas" }
			};

			for (String[] np : nuevosPermisos) {
				if (permisoRepositorio.findByNombre(np[0]).isEmpty()) {
					permisoRepositorio.save(new Permiso(np[0], np[1]));
				}
			}

			// Asignar los nuevos permisos a los roles correspondientes
			Rol adminRolAuditoria = rolRepositorio.findByNombre("ROLE_ADMIN").orElse(null);
			if (adminRolAuditoria != null) {
				for (String[] np : nuevosPermisos) {
					Permiso p = permisoRepositorio.findByNombre(np[0]).orElse(null);
					if (p != null && !adminRolAuditoria.getPermisos().contains(p)) {
						adminRolAuditoria.getPermisos().add(p);
					}
				}
				rolRepositorio.save(adminRolAuditoria);
			}

			Rol vetRolAuditoria = rolRepositorio.findByNombre("ROLE_VETERINARIO").orElse(null);
			if (vetRolAuditoria != null) {
				String[] vetPerms = {
						"VER_CLIENTES", "GESTIONAR_CLIENTES",
						"VER_PACIENTES", "GESTIONAR_PACIENTES",
						"VER_CITAS", "CREAR_CITAS", "EDITAR_CITAS", "AVANZAR_ESTADO_CITAS", "GESTIONAR_CITAS",
						"VER_HISTORIAL", "REGISTRAR_ATENCION",
						"GESTIONAR_HOSPITALIZACION",
						"VER_VENTAS", "CREAR_VENTAS", "REALIZAR_VENTAS",
						"VER_CAJA", "OPERAR_CAJA",
						"VER_FINANZAS"
				};
				for (String np : vetPerms) {
					Permiso p = permisoRepositorio.findByNombre(np).orElse(null);
					if (p != null && !vetRolAuditoria.getPermisos().contains(p)) {
						vetRolAuditoria.getPermisos().add(p);
					}
				}
				// Asegurar que no tenga permisos de Admin o Recepcionista que no le tocan
				rolRepositorio.save(vetRolAuditoria);
			}

			Rol recRolAuditoria = rolRepositorio.findByNombre("ROLE_RECEPCIONISTA").orElse(null);
			if (recRolAuditoria != null) {
				String[] recPerms = {
						"VER_CLIENTES", "GESTIONAR_CLIENTES",
						"VER_PACIENTES", "GESTIONAR_PACIENTES",
						"VER_CITAS", "CREAR_CITAS", "EDITAR_CITAS", "AVANZAR_ESTADO_CITAS", "GESTIONAR_CITAS",
						"VER_VENTAS", "CREAR_VENTAS", "REALIZAR_VENTAS",
						"VER_CAJA", "OPERAR_CAJA",
						"VER_FINANZAS"
				};

				// Eliminar permisos que no le corresponden
				String[] permsRemover = { "GESTIONAR_HOSPITALIZACION", "VER_HISTORIAL", "REGISTRAR_ATENCION",
						"CREAR_REGISTROS_CLINICOS" };
				for (String pRem : permsRemover) {
					Permiso pObj = permisoRepositorio.findByNombre(pRem).orElse(null);
					if (pObj != null) {
						recRolAuditoria.getPermisos().remove(pObj);
					}
				}

				for (String np : recPerms) {
					Permiso p = permisoRepositorio.findByNombre(np).orElse(null);
					if (p != null && !recRolAuditoria.getPermisos().contains(p)) {
						recRolAuditoria.getPermisos().add(p);
					}
				}
				rolRepositorio.save(recRolAuditoria);
			}

			// 2. Inicializar Sede por defecto
			Sede sedePrincipal;
			if (sedeRepositorio.count() == 0) {
				sedePrincipal = new Sede();
				sedePrincipal.setNombre("Sede Central");
				sedePrincipal.setDireccion("Av. Principal 123");
				sedePrincipal.setTelefono("999999999");
				sedePrincipal.setActivo(true);
				sedePrincipal = sedeRepositorio.save(sedePrincipal);
				System.out.println(" Sede inicializada");
			} else {
				sedePrincipal = sedeRepositorio.findAll().get(0);
			}

			// 3. Inicializar Usuario Admin y Empleado
			if (usuarioRepositorio.findByEmail("admin@veterinaria.com").isEmpty()) {
				// Buscar el rol ADMIN
				Rol adminRol = rolRepositorio.findByNombre("ROLE_ADMIN")
						.orElseThrow(() -> new RuntimeException("Error: Rol ADMIN no encontrado"));

				// Crear Usuario
				Usuario adminUsuario = new Usuario();
				adminUsuario.setNombre("Administrador");
				adminUsuario.setApellido("Sistema");
				adminUsuario.setDni("00000000"); // DNI ficticio
				adminUsuario.setTelefono("999999999");
				adminUsuario.setEmail("admin@veterinaria.com");
				adminUsuario.setPassword(passwordEncoder.encode("admin123"));
				adminUsuario.setActivo(true);
				Set<Rol> roles = new HashSet<>();
				roles.add(adminRol);
				adminUsuario.setRoles(roles);
				adminUsuario = usuarioRepositorio.save(adminUsuario);

				// Crear Empleado asociado
				Empleado adminEmpleado = new Empleado();
				adminEmpleado.setSueldoBase(new BigDecimal("3000.00"));
				adminEmpleado.setActivo(true);
				adminEmpleado.setUsuario(adminUsuario);

				// Agregar sede
				Set<Sede> sedes = new HashSet<>();
				sedes.add(sedePrincipal);
				adminEmpleado.setSedes(sedes);

				empleadoRepositorio.save(adminEmpleado);

				System.out.println(" Usuario administrador y empleado creados (admin@veterinaria.com / admin123)");
			}

			// 4. Inicializar Categorías de Producto
			if (categoriaProductoRepositorio.count() == 0) {
				categoriaProductoRepositorio.save(new CategoriaProducto(null, "Retail y Pet Shop",
						"Alimentos, accesorios, premios y snacks", true));
				categoriaProductoRepositorio.save(new CategoriaProducto(null, "Farmacia Veterinaria",
						"Medicamentos, antiparasitarios y biológicos", true));
				categoriaProductoRepositorio.save(new CategoriaProducto(null, "Insumos Clínicos",
						"Material descartable, suministros quirúrgicos y reactivos", true));
				categoriaProductoRepositorio.save(new CategoriaProducto(null, "Higiene y Cuidado",
						"Champús, sprays y productos de limpieza", true));
				System.out.println(" Categorías de producto inicializadas");
			}

			// 5. Inicializar Unidades de Medida
			if (unidadMedidaRepositorio.count() == 0) {
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Unidad", "Un", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Caja", "Cj", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Tableta", "Tab", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Blíster", "Blíst", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Mililitro", "ml", true, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Frasco", "Fr", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Saco", "Sc", false, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Kilogramo", "kg", true, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Metro", "m", true, true));
				unidadMedidaRepositorio.save(new UnidadMedida(null, "Kit / Prueba", "Kit", false, true));
				System.out.println(" Unidades de medida inicializadas");
			}

			// 6. Inicializar Proveedores de ejemplo
			if (proveedorRepositorio.count() == 0) {
				proveedorRepositorio.save(new Proveedor(null, "Distribuidora VetPharma S.A.C.", "20512345678",
						"Juan Pérez", "987654321", "ventas@vetpharma.com", "Av. Los Olivos 456", true));
				proveedorRepositorio.save(new Proveedor(null, "Pet Food Importaciones E.I.R.L.", "20587654321",
						"María López", "912345678", "contacto@petfood.com", "Jr. Comercio 789", true));
				System.out.println(" Proveedores de ejemplo inicializados");
			}

			// 7. Inicializar Especies
			if (especieRepositorio.count() == 0) {
				especieRepositorio.save(new Especie(null, "CANINO", true));
				especieRepositorio.save(new Especie(null, "FELINO", true));
				especieRepositorio.save(new Especie(null, "AVE", true));
				especieRepositorio.save(new Especie(null, "ROEDOR", true));
				especieRepositorio.save(new Especie(null, "REPTIL", true));
				especieRepositorio.save(new Especie(null, "EXÓTICO", true));
				System.out.println(" Especies inicializadas");
			}

			// 8. Inicializar Servicios Médicos
			if (servicioMedicoRepositorio.count() == 0) {
				servicioMedicoRepositorio.save(new ServicioMedico(null, "Consulta General",
						"Revisión general del paciente", new BigDecimal("50.00"), 30, 10, TipoServicio.CONSULTA, true));
				servicioMedicoRepositorio
						.save(new ServicioMedico(null, "Vacunación", "Aplicación de vacunas preventivas",
								new BigDecimal("40.00"), 20, 5, TipoServicio.VACUNACION, true));
				servicioMedicoRepositorio
						.save(new ServicioMedico(null, "Desparasitación Interna", "Administración de antiparasitarios",
								new BigDecimal("30.00"), 15, 5, TipoServicio.CONSULTA, true));
				servicioMedicoRepositorio
						.save(new ServicioMedico(null, "Esterilización Felina", "Cirugía de esterilización para gatos",
								new BigDecimal("150.00"), 120, 30, TipoServicio.CIRUGIA, true));
				servicioMedicoRepositorio
						.save(new ServicioMedico(null, "Esterilización Canina", "Cirugía de esterilización para perros",
								new BigDecimal("200.00"), 120, 30, TipoServicio.CIRUGIA, true));
				servicioMedicoRepositorio.save(new ServicioMedico(null, "Baño y Corte", "Servicio completo de estética",
						new BigDecimal("60.00"), 90, 15, TipoServicio.ESTETICA, true));
				servicioMedicoRepositorio.save(new ServicioMedico(null, "Ecografía", "Examen de imagen por ultrasonido",
						new BigDecimal("80.00"), 30, 10, TipoServicio.EXAMEN, true));
				System.out.println("✅ Servicios médicos inicializados");
			}

			// 9. Inicializar Categorías y Tamaños de Jaula, y Jaulas
			if (categoriaJaulaRepositorio.count() == 0) {
				CategoriaJaula catNormal = categoriaJaulaRepositorio
						.save(new CategoriaJaula(null, "Recuperación Estándar",
								"Jaulas para recuperación sin cuidados intensivos", true, new BigDecimal("40.00")));
				CategoriaJaula catInfecciosos = categoriaJaulaRepositorio.save(new CategoriaJaula(null,
						"Aislamiento Infecciosos", "Jaulas aisladas para pacientes con enfermedades contagiosas", true,
						new BigDecimal("60.00")));
				CategoriaJaula catUCI = categoriaJaulaRepositorio.save(new CategoriaJaula(null, "UCI",
						"Unidad de cuidados intensivos", true, new BigDecimal("100.00")));

				TamanoJaula tamPequena = tamanoJaulaRepositorio.save(new TamanoJaula(null, "Pequeña", true));
				TamanoJaula tamMediana = tamanoJaulaRepositorio.save(new TamanoJaula(null, "Mediana", true));
				TamanoJaula tamGrande = tamanoJaulaRepositorio.save(new TamanoJaula(null, "Grande", true));

				// Crear algunas jaulas si no hay
				if (jaulaRepositorio.count() == 0) {
					jaulaRepositorio.save(
							new Jaula(null, "J-01", catNormal, tamPequena, "DISPONIBLE", true, false, sedePrincipal));
					jaulaRepositorio.save(
							new Jaula(null, "J-02", catNormal, tamMediana, "DISPONIBLE", true, false, sedePrincipal));
					jaulaRepositorio.save(
							new Jaula(null, "J-03", catNormal, tamGrande, "DISPONIBLE", true, false, sedePrincipal));

					jaulaRepositorio.save(new Jaula(null, "I-01", catInfecciosos, tamMediana, "DISPONIBLE", true, true,
							sedePrincipal));

					jaulaRepositorio.save(
							new Jaula(null, "U-01", catUCI, tamMediana, "DISPONIBLE", true, false, sedePrincipal));
					System.out.println("✅ Jaulas y configuraciones de hospitalización inicializadas");
				}
			}

			// 10. Inicializar Productos Base
			if (productoRepositorio.count() == 0) {
				CategoriaProducto farmacia = categoriaProductoRepositorio.findAll().stream()
						.filter(c -> c.getNombre().equals("Farmacia Veterinaria")).findFirst().orElse(null);
				CategoriaProducto retail = categoriaProductoRepositorio.findAll().stream()
						.filter(c -> c.getNombre().equals("Retail y Pet Shop")).findFirst().orElse(null);

				UnidadMedida udCaja = unidadMedidaRepositorio.findAll().stream()
						.filter(u -> u.getNombre().equals("Caja")).findFirst().orElse(null);
				UnidadMedida udUnidad = unidadMedidaRepositorio.findAll().stream()
						.filter(u -> u.getNombre().equals("Unidad")).findFirst().orElse(null);
				UnidadMedida udSaco = unidadMedidaRepositorio.findAll().stream()
						.filter(u -> u.getNombre().equals("Saco")).findFirst().orElse(null);
				UnidadMedida udKg = unidadMedidaRepositorio.findAll().stream()
						.filter(u -> u.getNombre().equals("Kilogramo")).findFirst().orElse(null);
				UnidadMedida udTableta = unidadMedidaRepositorio.findAll().stream()
						.filter(u -> u.getNombre().equals("Tableta")).findFirst().orElse(null);

				if (farmacia != null && retail != null && udCaja != null && udUnidad != null) {
					productoRepositorio.save(new Producto(null, "Bravecto 10-20kg",
							"Antiparasitario externo e interno para perros medianos", new BigDecimal("120.00"), "MSD",
							farmacia, udCaja, udTableta, new BigDecimal("1"), null, true));
					productoRepositorio.save(new Producto(null, "NexGard Spectra 7.5-15kg",
							"Antiparasitario en tableta masticable", new BigDecimal("85.00"), "Boehringer Ingelheim",
							farmacia, udCaja, udTableta, new BigDecimal("3"), null, true));
					productoRepositorio.save(new Producto(null, "Pro Plan Adulto Razas Medianas 15kg",
							"Alimento premium para perros adultos", new BigDecimal("250.00"), "Purina", retail, udSaco,
							udKg, new BigDecimal("15"), null, true));
					productoRepositorio.save(new Producto(null, "Collar Isabelino N°4",
							"Collar de recuperación de plástico", new BigDecimal("15.00"), "Genérico", retail, udUnidad,
							udUnidad, new BigDecimal("1"), null, true));
					productoRepositorio.save(new Producto(null, "Shampoo Hipoalergénico 250ml",
							"Shampoo suave para pieles sensibles", new BigDecimal("35.00"), "Vetnova", retail, udUnidad,
							udUnidad, new BigDecimal("1"), null, true));
					System.out.println(" Productos de ejemplo inicializados");
				}
			}

		};
	}
}