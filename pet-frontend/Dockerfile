# Etapa 1: Construcción
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

# Etapa 2: Servidor Web
FROM nginx:alpine
# Copiar configuración personalizada de nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copiar los archivos compilados. Nota: En Angular 17+, la carpeta de salida suele ser dist/[nombre-proyecto]/browser
# Verifica esto si falla, pero asumo que es dist/pet-frontend/browser
COPY --from=build /app/dist/pet-frontend/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
