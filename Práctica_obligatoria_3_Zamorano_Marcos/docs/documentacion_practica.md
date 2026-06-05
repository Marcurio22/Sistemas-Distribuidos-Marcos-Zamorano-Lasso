# Documentación de la práctica — El Plantío 360

**Autor:** Marcos Zamorano Lasso  
**Asignatura:** Sistemas Distribuidos  
**Práctica:** Práctica obligatoria 3  
**Universidad:** Universidad de Burgos  
**Curso:** 2025/2026  

## 1. Objetivo de la práctica

El objetivo de esta práctica es desarrollar una aplicación web distribuida con una interfaz visual cuidada, autenticación segura, funcionalidades CRUD, integración entre backend y frontend, comunicación con servicios externos y despliegue completo mediante Docker.

El proyecto resultado es **El Plantío 360**, una plataforma académica inspirada en el entorno del Burgos CF y orientada a aficionados. Desde la aplicación se pueden consultar jugadores, revisar partidos, comprar entradas, comprar productos de tienda, visualizar un mapa interactivo del estadio, consultar sensores simulados, utilizar un asistente FAQ/IA y participar en un chat en tiempo real.

La intención no ha sido únicamente crear una web estética, sino integrar varios componentes propios de sistemas distribuidos:

- Spring Boot como aplicación principal;
- Flask como microservicio externo;
- MySQL como persistencia;
- Redis como caché;
- RabbitMQ como sistema de mensajería;
- MailHog como servidor SMTP simulado;
- WebSockets para comunicación en tiempo real;
- SonarQube para calidad de código;
- Docker Compose para despliegue completo.

## 2. Tecnologías utilizadas

La aplicación emplea las siguientes tecnologías:

- **Java 21**: lenguaje principal de la aplicación Spring.
- **Spring Boot**: framework principal del backend y frontend web.
- **Spring MVC**: controladores y rutas web.
- **Thymeleaf**: motor de plantillas del frontend.
- **Spring Security**: protección de rutas, autenticación y roles.
- **JWT**: autenticación stateless mediante token.
- **Spring Data JPA**: acceso a datos.
- **Hibernate**: implementación ORM.
- **Lombok**: reducción de código repetitivo con getters, setters y builders.
- **MySQL 8**: base de datos relacional.
- **Redis**: caché distribuida.
- **RabbitMQ**: broker de mensajería.
- **MailHog**: servidor de correo local para pruebas.
- **Flask**: microservicio externo de sensores.
- **Leaflet + OpenStreetMap**: visor cartográfico sin API keys.
- **WebSockets + SockJS + STOMP**: chat en tiempo real.
- **Tailwind CSS + DaisyUI**: diseño visual de la aplicación.
- **Docker Compose**: despliegue del entorno completo.
- **SonarQube**: análisis de calidad.
- **Postman**: pruebas manuales de API REST.

## 3. Funcionalidades principales

### 3.1. Página principal

La home presenta la plataforma, próximos partidos, jugadores destacados, tienda y acceso al asistente. Está pensada para que el usuario entienda rápidamente qué ofrece la aplicación.

### 3.2. Autenticación y usuarios

La aplicación permite:

- registro de usuarios;
- login;
- logout;
- acceso a zona privada;
- roles `ROLE_USER` y `ROLE_ADMIN`;
- protección de rutas privadas;
- uso de JWT.

Las contraseñas no se almacenan en claro, sino hasheadas mediante BCrypt.

### 3.3. Panel de usuario

El usuario autenticado puede acceder a `Mi espacio`, donde consulta:

- entradas;
- pedidos;
- notificaciones;
- asistente;
- muro blanquinegro.

### 3.4. Panel de administración

El administrador puede gestionar:

- usuarios;
- jugadores;
- partidos;
- productos;
- puntos de mapa;
- sensores;
- FAQs;
- pedidos;
- logs de IA.

El panel también incluye exportaciones CSV/PDF para los listados principales.

### 3.5. Jugadores

La página de jugadores muestra la plantilla con tarjetas visuales, imágenes, estado, dorsal, posición, nacionalidad y estadísticas simuladas.

### 3.6. Partidos y entradas

La aplicación muestra partidos programados y permite comprar entradas mediante una pasarela simulada. La compra reduce la disponibilidad de entradas y genera un pedido.

### 3.7. Tienda

La tienda permite comprar productos ficticios como camisetas, bufanda, taza o pack de día de partido. El sistema controla el stock y evita comprar más unidades de las disponibles.

### 3.8. Visor cartográfico

El mapa se implementa con Leaflet y OpenStreetMap. Muestra puntos de interés y sensores.

### 3.9. Sensores y Flask

Flask genera lecturas simuladas de sensores. Spring puede sincronizarlas, guardarlas en MySQL y mostrarlas en el mapa.

### 3.10. RabbitMQ y MailHog

Cuando se realiza una compra, la aplicación publica un evento en RabbitMQ. Posteriormente se genera un email simulado que puede verse desde MailHog.

### 3.11. Asistente IA

El asistente Blanquinegro Bot consulta primero las FAQs internas. Si está configurada la API key de Gemini, puede consultar Gemini usando contexto de la aplicación.

### 3.12. WebSockets

El Muro Blanquinegro permite mensajes en tiempo real entre usuarios mediante WebSockets, SockJS y STOMP.

## 4. Arquitectura general

La arquitectura puede resumirse así:

```text
Navegador
  |
  | HTTP / WebSocket
  v
Spring Boot + Thymeleaf
  |---- MySQL
  |---- Redis
  |---- RabbitMQ
  |---- MailHog
  |---- Flask
  |---- Gemini opcional
```

Spring Boot actúa como núcleo del sistema y coordina el resto de servicios.

## 5. Estructura del proyecto

La estructura principal es:

```text
Práctica_obligatoria_3_Zamorano_Marcos/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
├── docs/
│   ├── arquitectura.md
│   ├── documentacion_practica.md
│   ├── prompt_depuracion_admin.md
│   └── capturas/
├── postman/
│   └── Plantio360.postman_collection.json
├── python-api/
│   ├── app.py
│   ├── Dockerfile
│   └── requirements.txt
└── src/
    └── main/
        ├── java/com/marcos/plantio360/
        └── resources/
            ├── static/
            ├── templates/
            └── application.properties
```

## 6. Cómo ejecutar la práctica

### 6.1. Requisitos previos

Se necesita:

- Docker Desktop o Docker Engine con Docker Compose;
- opcionalmente Java 21 y Maven si se quiere ejecutar fuera de Docker.

### 6.2. Arranque del entorno

Desde la raíz:

```bash
docker compose up -d --build
```

### 6.3. Parada del entorno

```bash
docker compose down
```

Para borrar volúmenes:

```bash
docker compose down -v
```

## 7. URLs principales

| Servicio | URL |
|---|---|
| Aplicación Spring | `http://127.0.0.1:8080` |
| Health Spring | `http://127.0.0.1:8080/actuator/health` |
| Flask health | `http://127.0.0.1:5000/health` |
| Flask sensores | `http://127.0.0.1:5000/api/sensors` |
| RabbitMQ | `http://127.0.0.1:15672` |
| MailHog | `http://127.0.0.1:8025` |
| SonarQube | `http://127.0.0.1:9000` |

## 8. Usuarios de prueba

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | `admin@plantio360.local` | `admin1234` |
| Usuario | `user@plantio360.local` | `user1234` |

## 9. Rutas web relevantes

### Públicas

```text
/
/login
/register
/players
/players/{id}
/matches
/matches/{id}
/shop
/shop/{id}
/map
```

### Usuario autenticado

```text
/dashboard
/profile
/orders
/my-tickets
/assistant
/chat
```

### Administrador

```text
/admin
/admin/users
/admin/players
/admin/matches
/admin/products
/admin/map-points
/admin/sensors
/admin/faqs
/admin/orders
/admin/assistant-logs
```

## 10. API REST y Postman

La colección se encuentra en:

```text
postman/Plantio360.postman_collection.json
```

Endpoints principales:

```text
POST /api/auth/login
POST /api/auth/register
GET  /api/players
GET  /api/matches
GET  /api/products
GET  /api/map-points
GET  /api/sensors/latest
POST /api/sensors/sync
POST /api/assistant
POST /api/checkout
GET  /api/me/orders
GET  /api/me/tickets
```

## 11. Comprobación de servicios

### Flask

1. Entrar en `http://127.0.0.1:5000/health`.
2. Entrar en `http://127.0.0.1:5000/api/sensors`.
3. Entrar en `/map` desde Spring.
4. Pulsar sincronizar sensores Flask.
5. Ver que aparecen o se actualizan sensores en el mapa.

### RabbitMQ

1. Entrar en `http://127.0.0.1:15672`.
2. Login con `plantio / plantio`.
3. Hacer una compra en la web.
4. Revisar colas/intercambios y eventos.

### MailHog

1. Entrar en `http://127.0.0.1:8025`.
2. Hacer una compra.
3. Comprobar que aparece un correo simulado.

### Redis

1. Ver que el contenedor `redis` está healthy.
2. Consultar pantallas de catálogo.
3. Revisar logs si se quiere justificar caché.

### WebSockets

1. Abrir `/chat` con un usuario autenticado.
2. Enviar mensajes.
3. Abrir la misma ruta en otra pestaña o navegador.
4. Comprobar actualización en tiempo real.

## 12. Capturas recomendadas

Se recomienda añadir capturas en `docs/capturas/`.

Capturas sugeridas:

- `home.png` — página principal.
- `login.png` — pantalla de inicio de sesión.
- `register.png` — pantalla de registro.
- `players.png` — plantilla de jugadores.
- `player-detail.png` — ficha individual de jugador.
- `matches.png` — listado de partidos.
- `match-checkout.png` — compra de entrada.
- `shop.png` — tienda.
- `product-checkout.png` — compra de producto.
- `map.png` — visor Leaflet.
- `flask-health.png` — health de Flask.
- `flask-sensors.png` — sensores Flask.
- `mailhog-email.png` — correo de compra.
- `rabbitmq-queues.png` — panel RabbitMQ.
- `postman-login.png` — login desde Postman.
- `postman-sensors-sync.png` — sincronización de sensores desde Postman.
- `sonarqube.png` — análisis SonarQube.
- `docker-ps.png` — contenedores levantados.

## 13. Gestión de errores

La aplicación usa toasts flotantes para errores de negocio. La intención es evitar páginas de error genéricas cuando se trata de validaciones controlables.

Ejemplos de errores que deberían tratarse de forma específica:

- stock insuficiente;
- dorsal repetido;
- imagen con formato inválido;
- imagen demasiado grande;
- campos obligatorios vacíos;
- fecha de partido inválida;
- email duplicado;
- usuario no autenticado.

## 14. Seguridad

La seguridad está resuelta con:

- Spring Security;
- JWT;
- cookie HttpOnly para MVC;
- Bearer token para REST;
- roles;
- BCrypt para contraseñas;
- rutas protegidas.

## 15. Decisiones técnicas destacadas

### 15.1. Flask como microservicio externo

Aunque los sensores puedan editarse manualmente desde admin, Flask aporta valor distribuido porque representa un origen externo de datos. Spring consume esos datos mediante REST.

### 15.2. MailHog en lugar de SMTP real

Evita tener que configurar Gmail o credenciales externas. El profesor puede ver los correos directamente en el navegador.

### 15.3. Leaflet en lugar de Google Maps

No requiere API key, lo que hace que la práctica sea más fácil de ejecutar.

### 15.4. RabbitMQ para eventos

La compra no se limita a guardar datos, sino que produce eventos para demostrar comunicación asíncrona.

### 15.5. Docker Compose

Todo el entorno se levanta con un único comando.

## 16. Conclusión

El Plantío 360 integra los requisitos principales de la práctica y añade varios elementos valorables: mapas, pasarela simulada, IA, WebSockets, RabbitMQ, Redis, Flask, MailHog, Docker y SonarQube.

Con todo esto, se han cumplido todos los objetivos del enunciado y queda una práctica completa y consistente. Recordar aquí también que no se han podido satisfacer todas las restricciones de SonaQube, que queda para líneas futuras.