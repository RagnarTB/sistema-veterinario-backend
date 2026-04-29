# VetCare - Sistema Integral de Gestión Veterinaria (Enterprise Edition)

## 1. Arquitectura Base y Diseño del Sistema

El proyecto está construido bajo una **arquitectura RESTful multicapa** con separación estricta de responsabilidades, diseñada para **escalabilidad, mantenibilidad y seguridad**. Consta de un Backend robusto y un Frontend moderno e interactivo.

### Stack Tecnológico
- **Frontend**: Angular 18+, TailwindCSS, Angular Material, Google Identity Services
- **Backend**: Spring Boot 3+ (Java 21)
- **Persistencia**: Spring Data JPA + Hibernate
- **Base de datos de pruebas**: H2 en memoria
- **Base de datos producción**: PostgreSQL
- **Seguridad y Autenticación**: Spring Security + JWT + Google OAuth2
- **Integraciones Externas**: API Reniec (Perú) para validación de DNI, JavaMailSender para correos transaccionales
- **Testing**: JUnit 5 + MockMvc + Mockito
- **Metodología**: TDD (Test-Driven Development)

### Inteligencia Artificial (Próximamente)
> [!NOTE]  
> En una fase futura del proyecto se implementará una **Inteligencia Artificial** que actuará como **agente y asistente** tanto para el **veterinario, la (el) recepcionista, y el paciente**. Este agente integrará capacidades avanzadas de automatización, comunicación y asistencia de diagnóstico, simplificando radicalmente la operación diaria en todo el ecosistema de la clínica.

## 2. Modelos del Dominio (Total de 29)

El sistema agrupa y abarca los siguientes módulos clínicos, administrativos y de seguridad ya implementados en código:

**1. Módulo de Seguridad y Autenticación**
- `Usuario`, `Rol`, `RefreshToken`
- Autenticación Mixta: Credenciales clásicas (Email/Contraseña) y Single Sign-On (Google OAuth).
- Flujo de Auto-registro de clientes con validación por correo electrónico.

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
El corazón del código reside en los servicios existentes, abstrayendo la lógica de sus controladores. Recientemente se han validado exhaustivamente con suites de Test Unitarios aquellas partes clave que dictan la estabilidad de la operación clínica:
- `CitaServicio`: Motor de disponibilidad inteligente y cruce de horarios previniendo colisiones.
- `AtencionMedicaServicio`: Validación de reglas de las historias clínicas (tiempo máximo de edición <= 24 hrs por motivos legales).
- `HospitalizacionServicio`: Control de pacientes hospitalizados y su alta.
- `JaulaServicio`: Cambios de estado en jaulas y vinculación con hospitalizaciones vivas.
- `VentaServicio` & `CajaServicio`: Procesos transaccionales financieros.
- `AuthServicio` & `EmailServicio`: Gestión de seguridad, creación de cuentas inactivas, tokens de registro, vinculación de cuentas Google y notificaciones transaccionales.

## 4. Estado Actual del Proyecto

###  Funcionalidades Completadas
- [x] Aplicación Frontend Moderna y Responsiva (VetCare UI) con Angular.
- [x] Seguridad JWT + Roles Dinámicos Multi-acceso (Admin, Veterinario, Recepción, Cliente).
- [x] Autenticación y Registro de Clientes con Google Identity Services (SSO) y Correo Electrónico.
- [x] Integración pública con API de Reniec para Autocompletado de Datos por DNI.
- [x] Interfaz de Login inteligente y elegante que reconoce múltiples roles.
- [x] Directorio avanzado de clientes con soporte para estados (Pendiente, Activo, Inactivo).
- [x] Motor de Disponibilidad Inteligente de Citas y control de Días Bloqueados.
- [x] Control transaccional de punto de venta (Ventas, Caja, y Movimientos).
- [x] Gestión Médica (Consultas, Exámenes, Cirugías, Recetas, Vacunas, Desparasitaciones).
- [x] Control Operativo de Hospitalización y Monitoreo según Jaulas Disponibles por Sede.
- [x] Pruebas automatizadas configuradas (Mockito / MockMvc).

###  Pendientes Futuros
- [ ] Integración de la Inteligencia Artificial (Agente Veterinario/Recepcionista/Cliente).
- [ ] Dockerización y Pipeline de CI/CD.
- [ ] Reportes analíticos en formato PDF y Dashboard Avanzado.
- [ ] Sistema de recordatorios automáticos por WhatsApp/SMS.
