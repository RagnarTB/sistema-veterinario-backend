
# Sistema Veterinario API REST

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

### Módulos Implementados
Actualmente el sistema ya cuenta con estos módulos funcionales:

**Seguridad y autenticación**
- Usuario
- Rol
- JWT
- Registro/Login

**Gestión clínica**
- Cliente
- Paciente
- Especie
- Cita
- Atención médica
- Servicios médicos

**Horarios y agenda**
- Horarios veterinarios
- Días bloqueados

**Punto de venta (POS)**
- Producto
- Venta
- Detalle venta
- Caja diaria
- Movimientos de caja
- Apertura y cierre

## 2. Arquitectura por Capas (Vertical Slice)

Cada módulo sigue el patrón: **Controlador → Servicio → Repositorio → Entidad**

### 2.1 Controladores
Exponen endpoints REST limpios y desacoplados.

**Ejemplos implementados:**
```
- /api/pacientes
- /api/citas  
- /api/ventas
- /api/caja
- /api/auth
```

**Responsabilidades:**
- Validar entrada con `@Valid`
- Definir códigos HTTP correctos
- Delegar lógica al servicio
- Aplicar seguridad por roles con `@PreAuthorize`

### 2.2 Servicios
Aquí vive la **lógica de negocio real** del sistema.

**¿Por qué?**
La lógica no debe vivir en controladores ni repositorios porque:
- Facilita testing
- Mejora mantenibilidad
- Evita duplicación
- Protege reglas críticas del negocio

**Ejemplos reales implementados:**
- Cálculo de totales de venta
- Validación de stock
- Cálculo de disponibilidad de citas
- Cierre de caja
- Auditoría por usuario autenticado
- Bloqueo de agenda por días no laborables

### 2.3 Repositorios
Persistencia desacoplada usando `JpaRepository`.

**Beneficios:**
- CRUD automático
- Paginación
- Consultas derivadas
- Menor código boilerplate

### 2.4 DTOs (Seguridad y Contratos API)
Toda la API trabaja con **DTOs**.

**¿Por qué usamos DTOs?**
Porque evita:
- Exponer entidades
- Ciclos infinitos en JSON
- Fuga de datos sensibles
- Acoplamiento frontend ↔ base de datos

**Tipos implementados:**
- `RequestDTO`
- `ResponseDTO`
- DTOs especializados:
  - `AuthResponseDTO`
  - `SlotDisponibilidadDTO`
  - `CierreCajaResponseDTO`

## 3. CRUD Empresarial Implementado

El patrón CRUD ya está implementado en múltiples módulos:

| Módulo            | Estado       |
|-------------------|--------------|
| Pacientes         |[x]Implementado |
| Clientes          |[x]Implementado |
| Productos         |[x]Implementado |
| Citas             |[x]Implementado |
| Atención médica   |[x]Implementado |
| Servicios médicos |[x]Implementado |
| Especies          |[x]Implementado |

**Cada recurso sigue estándar REST:**
```
POST   /api/recurso
GET    /api/recurso
GET    /api/recurso/{id}
PUT    /api/recurso/{id}
DELETE /api/recurso/{id}
```

**Lógica aplicada:**
1. Validación de entrada
2. Conversión DTO → Entidad
3. Persistencia
4. Conversión Entidad → DTO
5. Respuesta HTTP correcta

**Características avanzadas implementadas:**
- Paginación con `Pageable`
- Respuestas tipo `Page<ResponseDTO>`
- Búsqueda individual
- Actualización completa
- Eliminación segura

## 4. Modelo Relacional y Relaciones JPA

La base de datos ya modela **relaciones reales del dominio veterinario**.

### Cliente → Paciente (1:N)
**Un cliente puede registrar múltiples mascotas.**

**Lógica:**
El frontend solo envía:
- `clienteId`
- `especieId`

El backend resuelve asociaciones reales.

**¿Por qué?**
Evita que el frontend manipule objetos completos y reduce errores.

### Cita → Atención Médica (1:1)
**Una cita genera una única atención clínica.**

**Beneficios:**
- Trazabilidad clínica
- Auditoría médica
- Continuidad del historial

### Venta → DetalleVenta → Producto
**Estructura real de ticket POS.**

**Diseño:**
- `Venta`: cabecera
- `DetalleVenta`: líneas
- `Producto`: catálogo + stock

**Lógica implementada:**
Cada detalle guarda:
- Precio unitario histórico
- Cantidad
- Subtotal

Esto evita que cambios futuros de precio alteren ventas antiguas.

## 5. Lógica de Negocio Avanzada

### 5.1 Motor de Disponibilidad de Citas
**Endpoint implementado:** `GET /api/citas/disponibilidad`

**Qué hace:**
Calcula horarios disponibles considerando:
- Horario del veterinario
- Duración del servicio
- Citas ya ocupadas
- Días bloqueados
- Fecha solicitada

**¿Por qué esta lógica es importante?**
Evita:
- Sobreposición de citas
- Citas en días cerrados
- Horas fuera de turno
- Colisiones entre servicios

**Esto lo convierte en una agenda inteligente real.**

### 5.2 Gestión de Estados de Cita
**Endpoint implementado:** `PATCH /api/citas/{id}/estado`

**Estados soportados:**
- `PENDIENTE`
- `CONFIRMADA`
- `CANCELADA`
- `ATENDIDA`

**Lógica:** Permite trazabilidad operacional de recepción y médicos.

### 5.3 Caja Diaria
**Endpoints implementados:**
```
POST /api/caja/abrir
PUT  /api/caja/cerrar
```

**Lógica de negocio:**
El cierre resume:
- Monto apertura
- Ingresos POS
- Movimientos
- Total final

**¿Por qué es importante?**
Profesionaliza el sistema para operación real en clínica.

### 5.4 Días Bloqueados y Horarios Veterinarios
**Días bloqueados:** Permite registrar feriados, mantenimiento, días no laborables, vacaciones.

**Horarios:** Define turnos por veterinario (hora inicio, hora fin, disponibilidad).

Esto alimenta directamente el motor de citas.

## 6. Seguridad JWT + Roles (RBAC)

### JWT Stateless
**La API funciona sin sesiones.** Cada request usa:
```
Authorization: Bearer <token>
```

**Flujo:**
1. Login
2. Generación token
3. Validación por filtro
4. Contexto de seguridad
5. Autorización por rol

### Roles implementados
| Rol                  | Descripción          |
|----------------------|----------------------|
| `ROLE_ADMIN`         | Administrador completo |
| `ROLE_CLIENTE`       | Cliente             |
| `ROLE_VETERINARIO`   | Veterinario         |
| `ROLE_RECEPCIONISTA` | Recepcionista       |

### Seguridad por método
**Seguridad fina con `@PreAuthorize`.**

**Ejemplos reales:**
- Solo veterinario crea atención médica
- Solo admin crea especies
- Admin y recepcionista abren caja
- Solo admin cierra caja
- Solo admin bloquea días
- Recepción/veterinario cambia estado de cita

### Auditoría de Identidad
**En operaciones sensibles, el backend obtiene el usuario desde JWT.**

**Caso real implementado:**
En atención médica:
- NO recibe veterinario en JSON
- Obtiene email desde `SecurityContext`

**Beneficio:** Previene suplantación de identidad.

## 7. Testing Real con TDD

**Tu proyecto ya tiene cobertura real de pruebas en:**

**Controladores:**
- `PacienteControllerTest`
- `ProductoControllerTest`
- `VentaControllerTest`
- `CajaControllerTest`
- `AuthControllerTest`
- `CitaControllerTest`
- `AtencionMedicaControllerTest`
- `EspecieControllerTest`

**Servicios:**
- `VentaServicioTest`
- `CajaServicioTest`

**Estrategia aplicada:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@MockBean
@WithMockUser
```

**¿Por qué esta arquitectura de testing?**
Spring Security + JWT necesita contexto completo. Permite:
- Probar rutas protegidas
- Validar HTTP real
- Simular roles
- Aislar servicios
- Mantener velocidad con H2

## 8. Estado Actual del Proyecto

###  Backend (Implementado)
- [x] Seguridad JWT + RBAC
- [x] CRUD pacientes
- [x] CRUD clientes
- [x] CRUD productos
- [x] CRUD citas
- [x] Disponibilidad inteligente
- [x] Atención médica
- [x] Gestión de especies
- [x] Horarios veterinarios
- [x] Días bloqueados
- [x] POS ventas
- [x] Caja diaria
- [x] Testing TDD

###  Pendiente
- [ ] Frontend Angular
- [ ] Dockerización
- [ ] CI/CD
- [ ] Deploy producción
- [ ] Reportes PDF
- [ ] Dashboard administrativo

## 9. Valor Arquitectónico del Proyecto

**Este sistema ya implementa patrones usados en software empresarial real:**

```
[x]Arquitectura multicapa
[x]DTO pattern
[x]RBAC
[x]JWT stateless
[x]TDD
[x]Transacciones
[x]Paginación
[x]Agenda inteligente
[x]POS real
[x]Caja diaria
[x]Auditoría de usuarios
[x]Separación de módulos por dominio

