# Guía Definitiva de Despliegue con Docker y DuckDNS

He preparado todo tu proyecto con la configuración necesaria para desplegarlo usando Docker. 

## 1. Archivos Creados
* **`pet/Dockerfile`**: Configurado en dos etapas (compila con Maven y ejecuta en Java 21 súper ligero).
* **`pet-frontend/Dockerfile`**: Configurado para compilar Angular y servirlo con un servidor web ultra-rápido.
* **`pet-frontend/nginx.conf`**: Servidor Nginx que actúa como **Proxy Inverso**. Todas las rutas `/api/*` se enviarán directamente al backend (Spring Boot), evitando problemas de CORS en producción.
* **`docker-compose.yml`**: Orquesta 4 contenedores: Postgres (BD), Redis (Caché), Backend (Spring) y Frontend (Angular).
* **`.env.example`**: Plantilla de variables de entorno seguras.

## 2. Dónde Desplegarlo Gratis (Recomendación)

> [!TIP]
> **Oracle Cloud (Always Free)** es el único proveedor que te da un VPS (Servidor Privado Virtual) lo suficientemente potente de manera vitalicia y gratuita. Te brindan una máquina ARM (Ampere A1) con hasta **4 núcleos y 24GB de RAM**.
> *Regístrate en Oracle Cloud, crea una instancia "Compute" seleccionando la imagen Ubuntu y la forma Ampere (Max. 4 OCPUs).*

## 3. Pasos de Despliegue en el Servidor (VPS)

Una vez que tengas acceso SSH a tu servidor (sea Oracle, AWS, o tu máquina):

1. **Instala Docker y Docker Compose:**
   ```bash
   sudo apt update
   sudo apt install docker.io docker-compose -y
   sudo usermod -aG docker $USER
   # Cierra sesión y vuelve a entrar para aplicar permisos
   ```

2. **Clona tu código y prepara el entorno:**
   ```bash
   # Clonar tu repositorio (asegúrate de haber subido estos cambios a github)
   git clone <URL_DE_TU_REPO>
   cd vet
   
   # Crea el archivo de entorno basado en el ejemplo
   cp .env.example .env
   # Edita el archivo .env y pon tus contraseñas reales
   nano .env
   ```

3. **¡Levanta el sistema!**
   ```bash
   docker-compose up -d --build
   ```
   *Esto descargará las imágenes, compilará tu backend y frontend en tiempo real, y encenderá la base de datos y redis. Tu aplicación estará disponible en el puerto 80 del servidor.*

## 4. Asignar un Dominio Gratuito con DuckDNS

Para no tener que entrar mediante una IP aburrida (ej: `143.12.33.2`), puedes usar [DuckDNS.org](https://www.duckdns.org/) para obtener un subdominio gratis (ej: `mi-veterinaria.duckdns.org`).

Para que tu servidor actualice automáticamente su IP pública en DuckDNS (muy útil si la IP de tu proveedor cambia), puedes agregar este 5to contenedor a tu archivo `docker-compose.yml` al final:

```yaml
  duckdns:
    image: lscr.io/linuxserver/duckdns:latest
    container_name: duckdns
    environment:
      - SUBDOMAINS=mi-veterinaria # Reemplaza con tu subdominio de DuckDNS
      - TOKEN=tu_token_de_duckdns # Reemplaza con el token que te da la web
      - TZ=America/Lima # Tu zona horaria
    restart: always
```

Simplemente edita tu `docker-compose.yml` agregando ese bloque, ejecuta `docker-compose up -d` y el contenedor se encargará de reportarle a DuckDNS cuál es tu IP cada 5 minutos.

> [!IMPORTANT]
> No olvides abrir el **Puerto 80 (HTTP)** y el **Puerto 443 (HTTPS)** en el firewall (Security Lists / Ingress Rules) de Oracle Cloud (o tu proveedor en la nube) para que el mundo exterior pueda acceder a tu Frontend.
