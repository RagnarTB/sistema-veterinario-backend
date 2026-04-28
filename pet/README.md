# Sistema Veterinario API REST (Enterprise Edition)

## 1. Arquitectura Base y Diseño del Sistema

El proyecto está construido bajo una **arquitectura RESTful multicapa** con separación estricta de responsabilidades, diseñada para **escalabilidad, mantenibilidad y seguridad**.

### Stack Tecnológico
- **Backend**: Spring Boot
- **Persistencia**: Spring Data JPA + Hibernate
- **Base de datos de pruebas**: H2 en memoria
- **Base de datos producción**: PostgreSQL
- **Seguridad**: Spring Security + JWT
- **Testing**: JUnit 5 + MockMvc + Mockito
- **Metodología**: TDD (Test-Driven Development)

### Inteligencia Artificial (Próximamente)
> [!NOTE]  
> En una fase futura del proyecto se implementará una **Inteligencia Artificial** que actuará como **agente y asistente** tanto para el **veterinario, la (el) recepcionista, y el paciente**. Este agente integrará capacidades avanzadas de automatización, comunicación y asistencia de diagnóstico, simplificando radicalmente la operación diaria en todo el ecosistema de la clínica.

## 2. Modelos del Dominio (Total de 29)

El sistema agrupa y abarca los siguientes módulos clínicos, administrativos y de seguridad ya implementados en código:

**1. Módulo de Seguridad y Autenticación**
- `Usuario`
- `Rol`
- `RefreshToken`

**2. Módulo Administrativo y de Personal**
- `Empleado`
- `Sede` (Multi-sede soportado)
- `HorarioVeterinario`
- `DiaBloqueado`

**3. Módulo de Gestión de Clientes y Pacientes**
- `Cliente`
- `Paciente`
- `Especie`

**4. Módulo de Agenda y Citas**
- `Cita`
- `ServicioMedico`

**5. Módulo Médico, Tratamientos y Recetas**
- `AtencionMedica` (Gestión principal del historial clínico)
- `Cirugia`
- `ConsentimientoInformado`
- `Vacuna`
- `Desparasitacion`
- `ExamenMedico`
- `RecetaMedica`
- `DetalleReceta`

**6. Módulo Avanzado de Hospitalización**
- `Hospitalizacion`
- `Jaula` (Control lógico de disponibilidad de espacios)
- `MonitoreoHospitalizacion` (Registro de signos vitales por horario)

**7. Módulo POS (Punto de Venta) y Finanzas**
- `InventarioSede`
- `Producto`
- `Venta`
- `DetalleVenta`
- `CajaDiaria`
- `MovimientoCaja`

## 3. Arquitectura por Capas (Vertical Slice)

Cada módulo sigue el patrón: **Controlador → Servicio → Repositorio → Entidad**

### 3.1 Servicios y Pruebas Unitarias
El corazón del código reside en los 28 servicios existentes, abstrayendo la lógica de sus controladores. Recientemente se han validado exhaustivamente con suites de Test Unitarios aquellas partes clave que dictan la estabilidad de la operación clínica:
- `CitaServicio`: Motor de disponibilidad inteligente y cruce de horarios previniendo colisiones.
- `AtencionMedicaServicio`: Validación de reglas de las historias clínicas (tiempo máximo de edición <= 24 hrs por motivos legales).
- `HospitalizacionServicio`: Control de pacientes hospitalizados y su alta.
- `JaulaServicio`: Cambios de estado en jaulas y vinculación con hospitalizaciones vivas.
- `VentaServicio` & `CajaServicio`: Procesos transaccionales financieros.

## 4. Estado Actual del Proyecto

###  Funcionalidades Completadas
- [x] Seguridad JWT + Roles (Admin, Veteriario, Recepción, Cliente)
- [x] Motor de Disponibilidad Inteligente de Citas y control de Días Bloqueados
- [x] Control transaccional de punto de venta (Ventas, Caja, y Movimientos)
- [x] Gestión Médica (Consultas, Exámenes, Cirugías, Recetas, Vacunas, Desparasitaciones)
- [x] Control Operativo de Hospitalización y Monitoreo según Jaulas Disponibles por Sede
- [x] Pruebas automatizadas configuradas (Mockito / MockMvc).

###  Pendientes Futuros
- [ ] Implementación de Frontend
- [ ] Integración de la Inteligencia Artificial (Agente Veterinario/Recepcionista/Cliente).
- [ ] Dockerización y CI/CD
- [ ] Reportes en formato PDF y Dashboard Avanzado
