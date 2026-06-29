# Docker deployment

Este proyecto queda preparado para correr en contenedores separados:

- `frontend`: Angular compilado servido por Nginx.
- `backend`: Spring Boot con Java 21.
- `db`: PostgreSQL 16.
- `redis`: Redis para cache.

## Variables

Copiar el archivo de ejemplo y completar los valores reales:

```bash
cp .env.example .env
```

Valores mínimos:

```env
POSTGRES_DB=VetCare
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
JWT_SECRET=secreto_para_jwt
MAIL_PASSWORD=password_de_aplicacion_gmail
GOOGLE_CLIENT_ID=client_id_google
FRONTEND_PORT=80
```

No subir `.env` al repositorio.

## Levantar localmente o en VPS

```bash
docker compose up -d --build
```

Ver logs:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

Detener:

```bash
docker compose down
```

Detener y borrar la base de datos local del compose:

```bash
docker compose down -v
```

## Puertos

Por defecto solo se publica el frontend:

- `http://localhost` o `http://IP_DEL_VPS`

El frontend proxy pasa `/api` hacia el backend interno, así que Angular usa `apiUrl: '/api'` en producción.

## VPS

Opción recomendada:

1. Instalar Docker y Docker Compose en la VPS.
2. Clonar el repo en la VPS.
3. Crear `.env` en la VPS.
4. Ejecutar `docker compose up -d --build`.
5. Apuntar tu dominio al IP de la VPS.
6. Poner HTTPS con un reverse proxy externo como Caddy, Nginx Proxy Manager o Traefik.

Para una primera prueba sin dominio, se puede entrar por `http://IP_DEL_VPS`.