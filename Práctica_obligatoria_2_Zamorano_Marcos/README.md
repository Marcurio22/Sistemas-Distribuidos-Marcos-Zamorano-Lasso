# Práctica obligatoria 2 — Sistemas Distribuidos

## Sistema distribuido de gestión de excepciones con Spring Boot, Flask y MySQL

Este proyecto implementa un sistema distribuido orientado al tratamiento de excepciones en varias capas de una aplicación. La práctica combina un frontend web desarrollado con *Spring Boot* y *Thymeleaf*, un servicio API independiente en *Python* con *Flask* y una base de datos *MySQL* desplegada mediante *Docker Compose*.

El objetivo principal no es únicamente construir una aplicación funcional, sino demostrar de forma clara cómo se capturan, propagan, traducen y presentan diferentes tipos de errores en un entorno distribuido. 

Esta aplicación permite trabajar con errores de ficheros, errores de acceso a base de datos, errores de llamadas a APIs externas y errores de red, todo ello tanto desde el navegador como desde *Postman*.

## 1. Objetivo de la práctica

La práctica parte de los siguientes requisitos:

* disponer de una página principal
* disponer de una pantalla de login sencilla
* disponer de una pantalla para simular invocaciones a un API de terceros mediante Python
* disponer de un API realizada en Python que acceda a una base de datos
* implementar y demostrar el tratamiento de excepciones en acceso a datos y en llamadas a servicios externos
* permitir la simulación de errores desde *Postman*
* traducir al frontend los errores no críticos con mensajes comprensibles para el usuario

A partir de esos requisitos, el proyecto se ha diseñado como un laboratorio interactivo de excepciones sobre una arquitectura distribuida sencilla, pero completa.

## 2. Tecnologías utilizadas

La solución final emplea las siguientes tecnologías y componentes:

* *Spring Boot*: para el frontend web y la lógica de integración con el servicio Python
* *Thymeleaf*: como motor de plantillas del frontend
* *Spring Security*: para el login, mantenimiento de sesión y logout seguro
* *Spring Data JPA*: para la persistencia de usuarios
* *Hibernate*: como implementación ORM bajo JPA
* *Python* + *Flask*: para el servicio API independiente
* *MySQL 8*: como base de datos
* *Docker Compose*: para levantar todos los servicios del entorno
* *Tailwind CSS* + *daisyUI*: para la parte visual del frontend
* *Postman*: para la validación manual de endpoints y simulación de errores

## 3. Arquitectura general del sistema

La aplicación está compuesta por tres bloques principales:

### 3.1. Spring Boot

Actúa como frontend y como backend web de integración. Desde aquí se gestiona:

* la página principal
* el login y el registro de usuarios
* el mantenimiento de la sesión
* el dashboard del usuario autenticado
* el laboratorio de pruebas `/lab`
* la invocación del servicio Flask mediante cliente HTTP
* la traducción de errores remotos a mensajes comprensibles para el usuario

### 3.2. Flask

Se ejecuta como un servicio independiente y expone endpoints pensados para demostrar distintos escenarios de excepción.

Desde Flask se realizan:

* lecturas de ficheros, tanto correctas como erróneas
* consultas a MySQL
* un error SQL forzado para simular fallo de acceso a datos
* llamadas a la PokeAPI como ejemplo de servicio externo
* una simulación de timeout para comprobar el tratamiento de errores de red

### 3.3. MySQL

Se utiliza para persistir usuarios, contraseñas cifradas, tokens de sesión y fecha del último login.

## 4. Decisiones técnicas adoptadas

Con este apartado busco exponer y justificar qué decisiones he tomado a nivel de diseño en aras de lograr una aplicación más consistente en conjunto.

### 4.1. Separación real entre frontend web y API externa

Aquí, he optado por separar claramente el frontend de Spring Boot del servicio Flask. Esto está directamente relacionado con la idea de la práctica, ya que, busco lograr una comunicación entre procesos distintos y no simplemente una aplicación monolítica.

### 4.2. Uso de JPA e Hibernate en la parte Java

La persistencia de usuarios la he implementado con *Spring Data JPA* sobre *Hibernate*. De este modo, no me limito a consumir una base de datos desde Python únicamente, sino que también, busco incorporar persistencia real en la parte de Java para los casos de:

* login
* registro
* almacenamiento del token de sesión
* actualización del último acceso

### 4.3. Traducción de errores en Spring

El servicio Flask devuelve errores estructurados. Spring los captura, los interpreta y los traduce a mensajes aptos para mostrarse en el frontend. Con esto, logro distinguir entre:

* información técnica útil para depuración
* información funcional comprensible para el usuario

### 4.4. Diferenciación entre errores críticos y no críticos

Aquí busco diferenciar entre errores críticos y no críticos. Por ejemplo:

* un fichero inexistente o un Pokémon inexistente son errores controlados y no críticos
* un timeout o un fallo de base de datos se consideran errores críticos

Esta clasificación puede verse tanto en las respuestas del API, como en la pantalla del laboratorio.

### 4.5. Persistencia del token de sesión

Tras un login correcto genero un token UUID y lo guardo en la tabla de usuarios. Esto permite demostrar que la sesión no solo existe en memoria HTTP, sino que queda reflejada en la base de datos.

### 4.6. Interfaz visual del laboratorio

Uno de mis objetivos era construir una interfaz más cuidada visualmente mediante *Tailwind CSS* y *daisyUI*, herramientas que he descubierto con el TFG.
Adicionalmente, he añadido:

* un menú lateral superpuesto con apertura y cierre
* un favicon personalizado
* un fondo degradado suave
* unas tarjetas de resultado claras
* una ficha visual avanzada para la PokeAPI

## 5. Estructura del proyecto

La estructura principal del repositorio es la siguiente:

```text
.
├── db/
│   └── init.sql
├── postman/
│   ├── practica2-local-environment.json
│   └── ... colección exportada ...
├── python-api/
│   ├── app/
│   │   ├── errors/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── __init__.py
│   │   └── main.py
│   ├── Dockerfile
│   └── requirements.txt
├── spring-app/
│   ├── src/main/java/com/marcoszamorano/practica2/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── dto/lab/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   ├── src/main/resources/
│   │   ├── static/
│   │   └── templates/
│   ├── src/main/tailwind/
│   ├── Dockerfile
│   ├── pom.xml
│   └── package.json
├── docker-compose.yml
└── README.md

```

## 6. Cómo descargar y ejecutar la práctica

### 6.1. Requisitos previos

Para ejecutar la práctica desde GitHub se necesita tener instalado:

* *Git*
* *Docker Desktop* o equivalente con soporte para *Docker Compose*
* opcionalmente *Java 17*, *Maven*, *Node.js* y *Python* si se desea trabajar también fuera de Docker

La forma recomendada de ejecución es mediante Docker, ya que así se evitan problemas de entorno y versiones.

### 6.2. Clonado del repositorio

En una terminal, clona el repositorio en el directorio deseado:

```bash
git clone https://github.com/Marcurio22/Sistemas-Distribuidos-Marcos-Zamorano-Lasso.git
cd Práctica_obligatoria_2_Zamorano_Marcos
```

### 6.3. Levantar el entorno completo

Desde la raíz del proyecto, ejecutar:

```bash
docker compose up -d --build
```

La primera ejecución tardará algo más, ya que se descargan dependencias de Maven, npm y Python.

### 6.4. Comprobar que todo está levantado

Se puede verificar con:

```bash
docker compose ps
```

También se pueden consultar los logs:

```bash
docker compose logs -f
```

Si solo interesa ver los logs de un servicio concreto:

```bash
docker compose logs -f spring-app
docker compose logs -f python-api
docker compose logs -f mysql-db
```


### 6.5. Limpieza y parada del entorno

Para detener los contenedores:

```bash
docker compose down
```

Para detenerlos y borrar también los volúmenes asociados:

```bash
docker compose down -v
```

## 7. Puertos y URLs principales

En esta práctica los servicios se exponen en:

* Spring Boot: `http://localhost:8081`
* Flask: `http://localhost:5000`
* MySQL: `localhost:3306`

Las URLs principales de uso son:

* Página principal: `http://localhost:8081/`
* Login: `http://localhost:8081/login`
* Registro: `http://localhost:8081/register`
* Dashboard: `http://localhost:8081/dashboard`
* Laboratorio: `http://localhost:8081/lab`
* Health de Flask: `http://localhost:5000/health`

## 8. Usuario de prueba y registro

El sistema incluye un usuario de prueba inicial sembrado al arrancar la aplicación:

```text
usuario: marcos
contraseña: marcos1234
```

Además, la aplicación dispone de una pantalla de registro que permite crear nuevos usuarios.

Durante el registro se validan:

* nombre de usuario obligatorio
* longitud del nombre de usuario
* e-mail obligatorio y con formato válido
* contraseña obligatoria
* confirmación de contraseña
* duplicidad de usuario y email

Las contraseñas se almacenan cifradas mediante *BCrypt*.

## 9. Base de datos y persistencia

La tabla principal utilizada es `users`. En ella se guardan, entre otros, los siguientes campos:

* `id`
* `username`
* `email`
* `password_hash`
* `role`
* `enabled`
* `session_token`
* `last_login_at`

Cuando un usuario inicia sesión correctamente:

* se genera un UUID de sesión
* se almacena en la base de datos
* se actualiza la fecha del último login

## 10. Endpoints del API Flask

El servicio Flask expone los siguientes endpoints relevantes:

### Health

* `GET /health`

Permite comprobar que el servicio Python está operativo.

### Ficheros

* `GET /api/lab/files/read/demo_ok.txt`
* `GET /api/lab/files/read/no_existe.txt`
* `GET /api/lab/files/read/confidential.txt`

Se utilizan para simular:

* lectura correcta de fichero
* fichero inexistente
* acceso prohibido simulado

### Base de datos

* `GET /api/lab/database/users`
* `GET /api/lab/database/force-error`

Se utilizan para:

* consultar usuarios desde Python
* provocar un error SQL controlado

### PokeAPI

* `GET /api/lab/pokemon/pikachu`
* `GET /api/lab/pokemon/pokemoninventadoxyz`
* `GET /api/lab/pokemon/<nombre>`

Se utilizan para:

* consulta correcta a API externa
* recurso inexistente
* búsqueda libre de Pokémon

### Red y timeout

* `GET /api/lab/network/timeout?seconds=10`

Este endpoint hace que Flask espere el tiempo indicado. El timeout real se produce cuando el cliente que lo invoca no está dispuesto a esperar tanto tiempo, por eso desde postman no nos aparecerá el error, pero desde el navegador, sí.

## 11. Laboratorio web `/lab`

La pantalla `/lab` es el núcleo de la práctica. Desde ella se puede demostrar, sin salir del navegador, todo el comportamiento distribuido del sistema.

### Casos disponibles

#### Ficheros

* lectura correcta
* fichero inexistente
* acceso prohibido

#### Base de datos

* consulta correcta de usuarios
* error SQL forzado

#### API externa

* consulta correcta a la PokeAPI
* Pokémon inexistente
* búsqueda libre de Pokémon

#### Red

* timeout entre Spring y Flask



En los casos de éxito se muestra además el JSON técnico recibido desde Flask, y en los casos de error también se puede mostrar el JSON técnico del error como bloque secundario plegable. Esta decisión se ha tomado porque `/lab` es una pantalla de laboratorio y no una pantalla de negocio estándar.


## 12. Tratamiento de excepciones

Esta es la parte central de la práctica, por eso creí conveniente explicarlo un poco.

### 12.1. En Flask

Flask encapsula los errores mediante excepciones personalizadas. Cada error se devuelve con una estructura homogénea, que incluye la siguiente información:

* categoría
* código de error
* mensaje para usuario
* mensaje técnico
* código HTTP
* criticidad
* timestamp

### 12.2. En Spring

Spring captura los errores remotos y los transforma en una representación adecuada para el frontend. Para ello se emplean:

* un servicio cliente HTTP para hablar con Flask
* una excepción propia para errores remotos
* un traductor de errores que convierte la respuesta técnica en mensajes comprensibles

## 13. Seguridad

Como he venido diciendo, la seguridad del sistema se ha resuelto con *Spring Security*.

En resumen y por no repetirme mucho, he implementado:

* pantalla de login personalizada
* logout con invalidación de sesión
* protección de rutas privadas
* persistencia del token UUID en base de datos
* cifrado de contraseñas con *BCrypt*

## 14. Postman

He preparado una carpeta `postman/` que almacena la colección de pruebas exportada.

### Colección

La colección de Postman se organiza en las siguientes carpetas:

* `Health`
* `Files`
* `Database`
* `Pokemon`
* `Network`
* `Spring Front`

## 15. Capturas

Tengo también una carpeta `docs/capturas/` con las siguientes imágenes para facilitar la corrección:

* home
* login
* registro
* menú lateral
* dashboard
* laboratorio con Pokémon correcto
* laboratorio con Pokémon incorrecto
* laboratorio con fichero ok
* laboratorio con error de fichero
* laboratorio con fichero prohibido
* laboratorio con consulta SQL ok
* laboratorio con error SQL
* laboratorio con timeout
* Postman health
* Postman error de base de datos
* Docker Desktop mostrando contenedores levantados
* Docker Compose up
* Tabla users en MySQL


## 16. Conclusiones

La práctica ha sido planteada para cumplir los requisitos técnicos del enunciado y los vistos en clase y para mejorar la presentación, la arquitectura y la capacidad de demostración del sistema.

Por ello he buscado no solo que la aplicación funcione, sino que el comportamiento de las excepciones quede visible, razonado y defendible desde el punto de vista de un sistema distribuido real.

Aún así, he pecado de no usar bibliotecas vistas en clase como *Lombok* por falta de rigurosidad a la hora de establecer el diseño técnico. Es una cosa que buscaré implementar para la práctica 3 y final de la asignatura.


Marcos Zamorano Lasso -
Sistemas Distribuidos -
Universidad de Burgos -
Curso 2025/2026
