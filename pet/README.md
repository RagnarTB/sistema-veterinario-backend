# Google Auth Backend

## Alcance de esta versión

- Google se usa para validar la identidad del cliente
- El sistema sigue usando su propio `JWT` y `refreshToken`
- El resto de la API no cambia su esquema de seguridad

## Enfoque elegido

No se implementó un flujo clásico de sesión web con `oauth2Login()`.

En este proyecto la autenticación ya era:

- API stateless
- `JWT` propio
- `refreshToken` propio
- Endpoints REST protegidos por Spring Security

Por eso se eligió este patrón:

1. El frontend obtiene un `idToken` de Google
2. El frontend envía ese token a `POST /api/auth/google`
3. El backend valida el token con Google
4. El backend busca o crea el usuario local
5. El backend emite el mismo `JWT` interno que usa el login normal

Este enfoque mantiene compatibilidad con el resto del sistema y evita mezclar seguridad de sesión con seguridad por token.

## Regla funcional principal

La identidad local del sistema sigue siendo el `email`.

Eso implica:

- No se crean cuentas duplicadas por proveedor
- Una cuenta Google y una cuenta local no deben terminar como dos usuarios separados con el mismo correo
- Si Google trae un email ya existente, se intenta reutilizar esa misma cuenta según las reglas del sistema

## Alcance aplicado

La integración de Google se pensó solo para clientes.

No se habilitó para:

- Administradores
- Recepcionistas
- Veterinarios
- Empleados en general

Si un correo ya pertenece a una cuenta que no tiene `ROLE_CLIENTE`, el acceso con Google se rechaza.

## Flujo actual

### Caso 1. Cliente nuevo con Google

1. Google devuelve un `idToken`
2. El backend valida el token
3. Se verifica que el correo esté validado por Google
4. Si no existe `Usuario` con ese email:
   - Se crea `Usuario`
   - Se asigna `ROLE_CLIENTE`
   - Se registra vínculo con Google
   - Se crea la entidad `Cliente`
5. Se devuelve `JWT + refreshToken + email + roles`

### Caso 2. Cliente existente con correo local y luego Google

1. Google devuelve un `idToken`
2. El backend valida el token
3. Encuentra un `Usuario` con el mismo email
4. Si el usuario tiene `ROLE_CLIENTE`:
   - Se vincula la cuenta Google a ese mismo usuario
   - No se crea una cuenta nueva
5. Se emite sesión normal del sistema

### Caso 3. Correo ya usado por una cuenta no cliente

1. Google devuelve un `idToken`
2. El backend encuentra el usuario por email
3. Si no tiene `ROLE_CLIENTE`, se rechaza el acceso con conflicto

Esto evita mezclar empleados o cuentas internas con el flujo público de clientes.

## Archivos principales involucrados

### `pet/src/main/java/com/veterinaria/controladores/AuthController.java`

Expone el endpoint:

- `POST /api/auth/google`

Su responsabilidad es solo delegar en el servicio de autenticación.

### `pet/src/main/java/com/veterinaria/servicios/AuthServicio.java`

Es el centro de la lógica de autenticación.

Aquí se implementó:

- `loginConGoogle(...)`
- Vinculación de Google a usuario existente
- Creación de cliente desde Google
- Construcción uniforme de `AuthResponseDTO`

También se dejó el armado de respuesta de sesión unificado para que:

- Login local
- Login Google
- Refresh token

Devuelvan el mismo contrato al frontend.

### `pet/src/main/java/com/veterinaria/servicios/GoogleTokenVerifierServicio.java`

Encapsula la validación del `idToken` de Google.

Responsabilidades:

- Validar que exista `google.oauth.client-id`
- Construir el verificador de Google
- Validar el token
- Exigir `email_verified`
- Extraer:
  - `subject`
  - `email`
  - `given_name`
  - `family_name`

La idea es que toda la lógica específica de Google quede concentrada aquí y no dispersa en el servicio principal.

### `pet/src/main/java/com/veterinaria/modelos/Usuario.java`

Se extendió el modelo con campos para soportar vinculación con Google:

- `googleSubject`
- `googleVinculado`

Además `email` quedó como único a nivel de base de datos para reforzar la política de una cuenta por correo.

### `pet/src/main/java/com/veterinaria/respositorios/ClienteRepositorio.java`

Se agregó búsqueda por `Usuario` para poder resolver si una cuenta cliente ya tiene perfil asociado o si hay que crearlo cuando entra por Google.

### `pet/src/main/java/com/veterinaria/seguridad/SecurityConfig.java`

Se habilitó el acceso público a:

- `POST /api/auth/google`

Más adelante también se ajustó CORS para permitir llamadas desde el frontend local.

### `pet/src/main/java/com/veterinaria/seguridad/UserDetailsServiceImpl.java`

Se ajustó la construcción de `UserDetails` para tolerar usuarios creados por Google sin password local.

Sin este cambio, una cuenta Google-only podía romper el flujo de autenticación secundaria porque Spring espera un password no nulo.

### `pet/src/main/java/com/veterinaria/dtos/GoogleLoginRequestDTO.java`

DTO mínimo para recibir:

- `idToken`

### `pet/src/main/java/com/veterinaria/dtos/AuthResponseDTO.java`

Más adelante se amplió para devolver también:

- `roles`

Esto fue necesario para que el frontend pudiera redirigir y filtrar vistas según el rol real del usuario autenticado.

## Reglas de negocio que quedaron implementadas

### 1. Una cuenta por email

No se permite que el mismo correo termine en dos usuarios distintos por usar proveedores diferentes.

### 2. Google solo para clientes

El flujo de Google no crea ni habilita empleados.

### 3. Solo correos verificados por Google

Si Google no marca el correo como verificado, el backend rechaza el login.

### 4. Sin password local automática

Las cuentas creadas desde Google no reciben una contraseña local por defecto.

Eso evita cuentas híbridas mal definidas desde el inicio.

### 5. Datos de cliente existentes se preservan

Si ya existe el perfil cliente, solo se completan campos vacíos seguros.

No se fuerza una sobrescritura agresiva de datos del dominio.

## Contrato esperado por frontend

Después de los ajustes posteriores, el backend responde con:

- `token`
- `refreshToken`
- `email`
- `roles`

Esto debe mantenerse sincronizado con el frontend.

Si se cambia `AuthResponseDTO`, revisar también:

- `frontend/pet-frontend/src/app/core/models/models.ts`
- `frontend/pet-frontend/src/app/core/services/auth.service.ts`

## Configuración requerida

En `pet/src/main/resources/application.properties`:

- `google.oauth.client-id=...`

Ese valor debe coincidir con el Client ID usado por el frontend.

Si el valor está vacío:

- El backend rechaza el flujo de Google

## CORS

Después de integrar el frontend se agregó configuración CORS en `SecurityConfig`.

Objetivo:

- Permitir llamadas desde Angular en desarrollo
- Evitar bloqueo de navegador sobre `/api/auth/google`

Orígenes permitidos actualmente:

- `http://localhost:4200`
- `http://127.0.0.1:4200`

Si cambia el host o puerto del frontend, actualizar esta lista.

## Qué revisar si Google deja de funcionar

### 1. Frontend

Confirmar:

- `googleClientId` en `environment.ts`
- `googleClientId` en `environment.prod.ts`
- Que el frontend esté usando el entorno correcto

### 2. Backend

Confirmar:

- `google.oauth.client-id`
- Que el backend haya reiniciado después de cambiar propiedades
- Que CORS permita el origen actual

### 3. Google Cloud Console

Confirmar:

- Origen autorizado del frontend
- Tipo correcto de cliente OAuth

### 4. Network / logs

Revisar:

- Si falla el render del botón
- Si falla el `POST /api/auth/google`
- Si el backend responde `401`, `409` o `500`

## Puntos de extensión futuros

Si más adelante se quiere ampliar esta integración, los caminos más razonables son:

### Activar password local en cuentas Google

No está implementado hoy.

La recomendación sería hacerlo con un flujo explícito, no automáticamente al crear la cuenta.

### Soportar más proveedores

No mezclar esa lógica en `AuthServicio` directamente.

Conviene replicar el patrón actual:

- Un validador por proveedor
- Una capa común de vinculación/creación local

### Ampliar datos del cliente

Si se quiere enriquecer el alta por Google, hacerlo con cuidado para no pisar datos sensibles del dominio como:

- DNI
- Teléfono
- Datos operativos ya corregidos manualmente

## Recomendaciones para futuros cambios

1. No cambiar el flujo a `oauth2Login()` salvo que el proyecto deje de ser una API stateless.

2. Mantener la política de una cuenta por email.

3. No reutilizar este flujo para empleados sin una decisión explícita de negocio.

4. Si se cambia el contrato de auth, mantener sincronizados backend y frontend.

5. Si se toca CORS, pensar tanto en desarrollo local como en despliegues reales.

6. Antes de depurar problemas de Google, validar primero configuración, origen y respuesta HTTP exacta.