<!--
  El Plantío 360 - README.
  Autor: Marcos Zamorano Lasso
  Práctica 3 - Sistemas Distribuidos
-->

# El Plantío 360

**Autor:** Marcos Zamorano Lasso  
**Asignatura:** Práctica 3 - Sistemas Distribuidos

El Plantío 360 es una aplicación web distribuida para aficionados al fútbol, inspirada en el entorno de El Plantío. Permite consultar jugadores y partidos, comprar entradas y productos mediante una pasarela simulada, visualizar un mapa interactivo con Leaflet/OpenStreetMap, sincronizar sensores simulados desde Flask, usar un asistente FAQ/IA, recibir notificaciones y probar un chat en tiempo real.

## Stack

- Java 21
- Spring Boot 4
- Spring MVC + Thymeleaf
- Spring Security stateless
- JWT en cookie HttpOnly y Bearer token REST
- JPA / Hibernate
- Lombok con `@Getter`, `@Setter` y `@Builder`
- MySQL
- Redis cache
- RabbitMQ
- MailHog
- WebSockets con STOMP/SockJS
- Flask para sensores simulados
- Leaflet + OpenStreetMap, sin API keys
- Docker Compose
- SonarQube
- Postman

## Arranque completo

```bash
docker compose up --build
```

URLs principales:

- Aplicación: http://localhost:8080
- RabbitMQ Management: http://localhost:15672
- MailHog: http://localhost:8025
- Flask sensores: http://localhost:5000/health
- SonarQube: http://localhost:9000

## Usuarios de prueba

| Rol | Email | Contraseña |
|---|---|---|
| Admin | admin@plantio360.local | admin1234 |
| Usuario | user@plantio360.local | user1234 |

## Funcionalidades defendibles

### Seguridad

La aplicación no usa sesión clásica de Spring como mecanismo principal. El login genera un JWT firmado con HMAC-SHA256. En MVC se guarda en cookie `PLANTIO360_JWT` HttpOnly y en REST se usa como `Authorization: Bearer <token>`.

### CRUDs

Incluye mantenimiento de:

- usuarios;
- jugadores;
- partidos;
- productos;
- puntos del mapa;
- sensores;
- FAQs;
- pedidos y logs de IA en modo consulta.

### Compra simulada

La compra de entradas y productos genera pedidos, decrementa stock/disponibilidad, crea entradas con QR simulado y publica un evento en RabbitMQ. El consumidor procesa el evento y envía email a MailHog.

### Visor cartográfico

El visor usa Leaflet y OpenStreetMap, por lo que no requiere API keys. Muestra puntos de interés y sensores simulados.

### Remote sensing

El microservicio Flask genera lecturas variables de parking, afluencia, humedad y temperatura. Spring puede sincronizarlas y persistirlas.

### IA

El asistente Blanquinegro Bot busca primero en FAQ local. Si se configura `PLANTIO_GEMINI_API_KEY` y `PLANTIO_GEMINI_ENABLED=true`, puede llamar a Gemini. Sin clave, queda operativo con fallback local.

### Redis

Se usa como caché para catálogos públicos: jugadores, partidos, productos y sensores recientes.

### WebSockets

El Muro Blanquinegro usa STOMP/SockJS para chat en tiempo real.

## SonarQube

Una vez arrancado SonarQube:

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN
```

## API REST básica

La colección está en:

```text
postman/Plantio360.postman_collection.json
```

Flujo recomendado:

1. `POST /api/auth/login`
2. copiar `token`
3. usar `Authorization: Bearer {{jwt}}`
4. probar checkout, pedidos y sensores.

## Observaciones de entrega

El proyecto incluye datos iniciales para que el profesor no tenga que configurar nada manualmente. La estética es ficticia y académica, sin uso de logotipos oficiales.
