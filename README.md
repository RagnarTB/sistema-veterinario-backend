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

### ✅ Funcionalidades Completadas
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

### ⏳ Pendientes Futuros
- [ ] Integración de la Inteligencia Artificial (Agente Veterinario/Recepcionista/Cliente).
- [ ] Dockerización
- [ ] Reportes analíticos en formato PDF y Dashboard Avanzado.
- [ ] Sistema de recordatorios automáticos por WhatsApp/SMS.

---

## 5. Guía de Despliegue en Azure (VPS + Docker Compose)

El despliegue de **VetCare** se realiza utilizando un esquema de contenedores orquestados con **Docker Compose**, aprovisionado sobre una Máquina Virtual (VPS) en **Azure for Students**. Esta arquitectura garantiza un entorno aislado, replicable y fácil de mantener.

### 5.1. Arquitectura de Despliegue
El orquestador levantará 3 servicios principales:
1. **Frontend (Angular)**: Servido a través de un contenedor ligero con NGINX.
2. **Backend (Spring Boot 3)**: Contenedor ejecutando el `.jar` compilado con Java 21.
3. **Base de Datos (PostgreSQL)**: Contenedor oficial de PostgreSQL con un volumen de datos persistente.

### 5.2. Preparación de la VPS en Azure
1. Ingresa al portal de Azure y crea un nuevo recurso: **Virtual Machine**.
2. **Imagen sugerida**: Ubuntu Server 22.04 LTS o 24.04 LTS.
3. **Tamaño**: Standard_B1s o Standard_B2s (recomendado para memoria suficiente al arrancar Java + BD + Angular).
4. **Autenticación**: Configura tu clave pública SSH.
5. **Redes (Reglas de puerto de entrada)**:
   Asegúrate de abrir los siguientes puertos en el Grupo de Seguridad de Red (NSG) asignado a la IP de la máquina:
   - `22` (SSH)
   - `80` (HTTP - Frontend)
   - `443` (HTTPS - Opcional para SSL)
   - `8080` (API Backend - Opcional, a menos que enrutes todo tras el NGINX del front)

### 5.3. Instalación de Docker y Docker Compose
Conéctate por SSH a tu VPS de Azure y ejecuta los comandos de instalación de Docker Engine y Docker Compose Plugin correspondientes a Ubuntu.

### 5.4. Configuración de los Archivos Docker

**1. Dockerfile del Backend** (Ubicación: `/pet/Dockerfile`)

```dockerfile
# Etapa 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Etapa 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**2. Dockerfile del Frontend** (Ubicación: `/pet-frontend/Dockerfile`)

```dockerfile
# Etapa 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build --configuration=production

# Etapa 2: Servir con Nginx
FROM nginx:alpine
COPY --from=build /app/dist/pet-frontend/browser /usr/share/nginx/html
COPY nginx-custom.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

> Nota: Crea un archivo `nginx-custom.conf` en el frontend para redirigir el tráfico al `index.html`, necesario para que funcionen las rutas de Angular al recargar la página.

**3. Orquestador: `docker-compose.yml`** (Ubicación: raíz del repositorio)

```yaml
version: '3.8'

services:
  # 1. Base de Datos PostgreSQL
  postgres-db:
    image: postgres:15-alpine
    container_name: vetcare-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: vetcare_db
      POSTGRES_USER: vetcare_user
      POSTGRES_PASSWORD: vetcare_password_super_segura
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    networks:
      - vetcare-network

  # 2. Backend Spring Boot
  backend:
    build:
      context: ./pet
    container_name: vetcare-backend
    restart: unless-stopped
    depends_on:
      - postgres-db
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-db:5432/vetcare_db
      - SPRING_DATASOURCE_USERNAME=vetcare_user
      - SPRING_DATASOURCE_PASSWORD=vetcare_password_super_segura
      - SPRING_JPA_HIBERNATE_DDL_AUTO=update
      - JWT_SECRET=poner_clave_fuerte_base64_aqui
      # Añadir variables de Reniec, Google OAuth y Correo aquí
    ports:
      - "8080:8080"
    networks:
      - vetcare-network

  # 3. Frontend Angular + Nginx
  frontend:
    build:
      context: ./pet-frontend
    container_name: vetcare-frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - vetcare-network

volumes:
  pgdata:

networks:
  vetcare-network:
    driver: bridge
```

### 5.5. Ejecución del Despliegue

1. Clona tu repositorio en la VPS de Azure:
   ```bash
   git clone https://github.com/ragnartb/sistema-veterinario.git
   cd sistema-veterinario
   ```
2. Reemplaza las variables de entorno en el `docker-compose.yml` con tus credenciales reales (Google Client ID, Tokens de Reniec, contraseñas SMTP, etc.).
3. Levanta todos los servicios en segundo plano:
   ```bash
   docker compose up -d --build
   ```
4. Verifica el estado:
   ```bash
   docker compose ps
   docker compose logs -f backend  # Para ver los logs de Spring Boot
   ```

Una vez finalizado, podrás acceder al frontend colocando la Dirección IP Pública de tu VPS de Azure en el navegador (`http://TU_IP_PUBLICA`).
