# Arquitectura técnica de El Plantío 360

**Autor:** Marcos Zamorano Lasso  
**Asignatura:** Sistemas Distribuidos  
**Proyecto:** Práctica obligatoria 3  
**Universidad:** Universidad de Burgos  

## 1. Introducción

El Plantío 360 es una aplicación web distribuida orientada a simular una plataforma inteligente para aficionados al fútbol. El sistema permite consultar la plantilla, revisar partidos, comprar entradas y productos mediante una pasarela ficticia, visualizar puntos y sensores en un visor cartográfico, usar un asistente FAQ/IA, participar en un muro en tiempo real y demostrar integración entre varios servicios desplegados mediante Docker Compose.

La arquitectura se ha planteado para que el profesor pueda levantar el entorno completo sin configuración manual adicional. La aplicación principal está desarrollada con Spring Boot y se apoya en servicios externos para persistencia, mensajería, caché, correo simulado, análisis de calidad y sensores simulados.

## 2. Vista global del sistema

La arquitectura general es la siguiente:

```text
Usuario / Navegador
        |
        | HTTP / HTML / CSS / JS / WebSocket
        v
Spring Boot + Thymeleaf + Spring Security + JPA
        |
        |------------------> MySQL
        |------------------> Redis
        |------------------> RabbitMQ
        |------------------> MailHog
        |------------------> Flask sensors API
        |------------------> Gemini API opcional
        |
        v
Leaflet + OpenStreetMap en frontend
```

Cada servicio tiene una responsabilidad clara:

| Componente | Responsabilidad principal |
|---|---|
| Spring Boot | Aplicación principal, seguridad, vistas, API REST, lógica de negocio |
| Thymeleaf | Renderizado servidor de las páginas HTML |
| Tailwind CSS + DaisyUI | Interfaz visual y componentes UI |
| MySQL | Persistencia relacional |
| JPA/Hibernate | Mapeo objeto-relacional |
| Redis | Caché y mejora de rendimiento |
| RabbitMQ | Procesamiento asíncrono de eventos |
| MailHog | Bandeja SMTP local para correos simulados |
| Flask | Microservicio de sensores simulados/remote sensing |
| Leaflet + OSM | Visor cartográfico sin API keys |
| WebSockets | Chat en tiempo real |
| Gemini | Respuesta IA opcional cuando la FAQ local no basta |
| SonarQube | Calidad y análisis estático del código |

## 3. Servicios Docker

El entorno se levanta mediante `docker-compose.yml`. Los servicios principales son:

```text
spring-app
python-api
mysql
redis
rabbitmq
mailhog
sonarqube
```

### 3.1. spring-app

Es el contenedor principal de la aplicación Java. Expone el puerto `8080` y contiene:

- controladores MVC;
- controladores REST;
- seguridad JWT;
- servicios de negocio;
- persistencia JPA;
- integración con RabbitMQ;
- integración con Redis;
- integración con Flask;
- integración con MailHog;
- integración opcional con Gemini;
- vistas Thymeleaf;
- recursos estáticos.

### 3.2. python-api

Es el microservicio Flask. Expone el puerto `5000` y se utiliza para simular sensores de remote sensing. Sus endpoints principales son:

```text
GET /health
GET /api/sensors
```

Desde fuera de Docker se puede probar con:

```text
http://127.0.0.1:5000/health
http://127.0.0.1:5000/api/sensors
```

Dentro de Docker, Spring no usa `localhost`, sino el nombre del servicio:

```text
http://python-api:5000
```

### 3.3. mysql

Base de datos relacional de la aplicación. Spring se conecta usando una URL JDBC interna del estilo:

```text
jdbc:mysql://mysql:3306/plantio360
```

En MySQL se guardan usuarios, jugadores, partidos, productos, pedidos, entradas, sensores, FAQs, mensajes de chat, logs de IA y notificaciones.

### 3.4. redis

Redis se utiliza como sistema de caché para datos de consulta frecuente. Su objetivo es demostrar un componente distribuido adicional y mejorar el rendimiento de ciertos catálogos públicos.

### 3.5. rabbitmq

RabbitMQ se usa para desacoplar la compra de entradas/productos del envío de notificaciones por correo. La aplicación publica eventos de compra y un consumidor los procesa posteriormente.

El panel de gestión está disponible en:

```text
http://127.0.0.1:15672
```

Credenciales:

```text
plantio / plantio
```

### 3.6. mailhog

MailHog simula un servidor SMTP y una bandeja de entrada web. Permite comprobar los emails enviados por la aplicación sin configurar Gmail ni credenciales reales.

URL:

```text
http://127.0.0.1:8025
```

### 3.7. sonarqube

SonarQube se incluye para demostrar análisis de calidad de código. Está disponible en:

```text
http://127.0.0.1:9000
```

## 4. Arquitectura interna de Spring Boot

La aplicación sigue una división clásica por capas:

```text
controller -> service -> repository -> model
```

### 4.1. Capa controller

Contiene controladores MVC y REST.

Controladores principales:

- `AuthController`: login, registro, logout y autenticación REST.
- `PageController`: páginas públicas y privadas principales.
- `AdminController`: panel de administración.
- `ApiController`: API REST de catálogo, sensores, checkout y datos de usuario.
- `AssistantController`: asistente FAQ/Gemini.
- `ChatController`: muro en tiempo real y gestión de mensajes.
- `AdminExceptionHandler`: gestión específica de errores de administración.

### 4.2. Capa service

Contiene la lógica de negocio:

- autenticación y usuarios;
- compras de productos y entradas;
- eventos RabbitMQ;
- envío de correos;
- sincronización de sensores Flask;
- caché Redis;
- asistente FAQ/IA;
- exportación CSV/PDF;
- consultas de catálogo.

### 4.3. Capa repository

Repositorios Spring Data JPA para acceder a MySQL:

- `AppUserRepository`;
- `PlayerRepository`;
- `FootballMatchRepository`;
- `ProductRepository`;
- `PlantioOrderRepository`;
- `TicketRepository`;
- `SensorReadingRepository`;
- `MapPointRepository`;
- `FaqRepository`;
- `ChatMessageRepository`;
- `AssistantLogRepository`;
- `NotificationRepository`.

### 4.4. Capa model

Entidades JPA principales:

- `AppUser`;
- `Player`;
- `FootballMatch`;
- `Product`;
- `PlantioOrder`;
- `OrderItem`;
- `Ticket`;
- `SensorReading`;
- `MapPoint`;
- `Faq`;
- `AssistantLog`;
- `ChatMessage`;
- `Notification`.

## 5. Seguridad

La seguridad está implementada con Spring Security y JWT.

### 5.1. Login MVC

El usuario inicia sesión desde `/login`. Si las credenciales son correctas, se genera un JWT y se guarda en una cookie `HttpOnly`.

### 5.2. API REST

La API REST puede usar el token en cabecera:

```text
Authorization: Bearer <token>
```

### 5.3. Logout

El logout elimina la cookie JWT y redirige al login o a la página correspondiente.

### 5.4. Roles

Hay dos roles principales:

```text
ROLE_USER
ROLE_ADMIN
```

El administrador puede acceder a `/admin/**`.

### 5.5. Contraseñas

Las contraseñas se guardan hasheadas con BCrypt. No se almacenan en texto plano.

## 6. Frontend

El frontend usa:

- Thymeleaf;
- Tailwind CSS;
- DaisyUI;
- JavaScript propio;
- Leaflet;
- SockJS/STOMP.

No se usa Bootstrap. Los componentes visuales se basan en clases de DaisyUI como `btn`, `card`, `badge`, `alert`, `table`, `modal`, `input`, `select` y `textarea`.

## 7. Visor cartográfico

El mapa está implementado con Leaflet y OpenStreetMap.

No se usa Google Maps ni API keys. Esto simplifica la defensa y evita configuración externa.

El visor muestra:

- estadio;
- parkings;
- puertas;
- zonas de seguridad;
- bares;
- puntos de interés;
- sensores simulados.

## 8. Flask y remote sensing

Flask representa un sistema externo de sensores. La idea no es que Flask sustituya el panel de administración, sino que simule una fuente externa de datos.

Flujo:

```text
Flask genera sensores
        ↓
Spring llama a Flask por REST
        ↓
Spring recibe JSON
        ↓
Spring transforma el JSON en entidades SensorReading
        ↓
Spring guarda en MySQL
        ↓
Leaflet muestra los datos
```

Esto permite defender una integración real entre dos tecnologías y procesos distintos: Java/Spring y Python/Flask.

## 9. RabbitMQ y MailHog

Cuando un usuario compra una entrada o producto:

1. Spring valida la compra.
2. Spring crea un pedido en MySQL.
3. Spring actualiza stock o disponibilidad.
4. Spring publica un evento en RabbitMQ.
5. Un consumidor procesa el evento.
6. Se genera un correo simulado.
7. El correo se ve en MailHog.

Esto demuestra comunicación asíncrona y desacoplamiento.

## 10. WebSockets

El Muro Blanquinegro usa WebSockets con SockJS y STOMP. Permite que varios usuarios vean mensajes en tiempo real.

El administrador puede moderar mensajes eliminándolos si los considera inapropiados.

## 11. Asistente IA

El asistente funciona en dos niveles:

1. Busca primero en FAQs internas.
2. Si Gemini está configurado y habilitado, puede consultar Gemini usando contexto interno de la aplicación.

Esto evita depender exclusivamente del LLM y permite respuestas controladas sobre funcionamiento de la plataforma.

## 12. Exportaciones

El panel de administración incluye exportación CSV y PDF para listados como usuarios, jugadores, partidos, productos, sensores, FAQs, pedidos y logs IA.

Los nombres de archivo están pensados para distinguir el contenido exportado.

## 13. Decisiones de diseño defendibles

- Docker Compose para evitar configuración manual.
- Leaflet/OpenStreetMap para no depender de API keys.
- MailHog para correo reproducible en local.
- RabbitMQ para desacoplar compra y notificación.
- Flask para simular un sistema externo de sensores.
- Redis para demostrar caché distribuida.
- JWT en cookie HttpOnly para rutas MVC y Bearer token para REST.
- DaisyUI/Tailwind para una interfaz moderna y coherente.
- Datos iniciales para que el sistema sea demostrable nada más arrancar.

