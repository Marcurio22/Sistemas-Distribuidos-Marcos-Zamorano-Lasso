# README

## Autor

Marcos Zamorano Lasso

## Resumen de la práctica

En esta práctica he completado la implementación de un chat distribuido cliente-servidor usando sockets TCP en Java,
siguiendo la estructura indicada en el enunciado y respetando la base del proyecto proporcionado.

La aplicación queda organizada en dos partes principales:

- un servidor central que acepta conexiones de clientes, recibe mensajes y los difunde al resto.
- varios clientes que se conectan al servidor, envían mensajes y escuchan en paralelo los mensajes entrantes.

La comunicación entre ambos lados se hace mediante objetos serializados con `ObjectInputStream` y `ObjectOutputStream`,
usando la clase `ChatMessage` como formato común.

---

## Cambios realizados

### 1. `ChatClientImpl.java`

He adaptado el cliente para que cumpla el comportamiento pedido en el enunciado:

- conexión al servidor usando el puerto fijo 1500.
- posibilidad de lanzar el cliente con:
  - `java es.ubu.lsi.client.ChatClientImpl username`
  - `java es.ubu.lsi.client.ChatClientImpl serverAddress username`
- mantenimiento de un hilo de escucha (`ChatClientListener`) para recibir mensajes mientras el hilo principal sigue leyendo desde consola.
- soporte para los comandos:
  - `logout`
  - `ban nickname`
  - `unban nickname`

#### Decisiones tomadas

La parte más importante del cliente ha sido la gestión de `ban` y `unban`.

He decidido que el bloqueo sea local al cliente, porque el enunciado realmente pide que, al banear a alguien, 
simplemente no se muestren sus mensajes. Eso significa que el servidor puede seguir retransmitiendo los mensajes con normalidad, 
y cada cliente decide cuáles enseñar y cuáles ocultar.

Para ello:

- guardo los usuarios baneados en una colección local.
- cuando llega un mensaje, el cliente intenta deducir quién es el emisor a partir del texto recibido.
- si ese emisor está baneado, el mensaje no se muestra por pantalla.

Además, cuando un usuario escribe `ban nickname`, el cliente no solo guarda ese baneo localmente, sino que también envía al servidor
el mensaje con el formato:

`usuarioActual ha baneado a usuarioBaneado`

---

### 2. `ChatServerImpl.java`

He completado la lógica del servidor para que actúe como un servidor central de difusión.

Funciones principales implementadas:

- apertura del `ServerSocket` en el puerto 1500.
- aceptación continua de clientes mientras el servidor siga activo.
- creación de un hilo independiente por cliente (`ServerThreadForClient`).
- lectura del nombre de usuario enviado por el cliente al conectar.
- asignación de un identificador a cada cliente.
- difusión de los mensajes al resto de clientes.
- gestión de desconexión con `logout`.

#### Decisiones tomadas

El modelo es el siguiente:

- el servidor tiene una lista de clientes conectados.
- cada cliente conectado se atiende en un hilo independiente.
- cuando llega un mensaje de tipo `MESSAGE`, el servidor lo reenvía.
- cuando llega `LOGOUT`, el servidor elimina al cliente y anuncia su salida.

Además, he añadido el registro de mensajes con el prefijo pedido:

`Marcos Zamorano Lasso patrocina el mensaje: ...`

Este log lo coloqué en el método de difusión porque es el punto exacto por el que pasan todos los mensajes que realmente se transmiten en
la aplicación.

También tuve cuidado con el caso del comando `ban`. Si el cliente envía el texto:

`pepe ha baneado a ana`

el servidor no debe reconstruirlo como si fuera un mensaje normal del tipo:

`pepe: pepe ha baneado a ana`

Por eso añadí una comprobación específica para reenviar ese mensaje con el formato correcto.

---

### 3. `pom.xml`

He dejado configurado Maven para compilar con Java 8, mediante:

- `maven.compiler.source = 1.8`
- `maven.compiler.target = 1.8`

---

### 4. `build.xml`

He ajustado el fichero de Ant para que el target `javadoc` genere la documentación en la carpeta `doc`.

Cambios principales:

- adaptación de la ruta de código fuente a la estructura Maven: `src/main/java`.
- uso de Java 8 también en la generación.
- simplificación del target `javadoc` para que funcione de forma directa.
---

## Decisiones técnicas relevantes

### Uso de `volatile`

He usado `volatile` en algunas banderas de control compartidas entre hilos, como por ejemplo, en el cliente y en el servidor.

El motivo es que esas variables se consultan desde más de un hilo, y me interesa asegurar que cuando un hilo cambie su valor, 
el resto vean ese cambio inmediatamente. No lo he usado para atomicidad, sino para visibilidad entre hilos.

### Orden de creación de streams

He creado primero `ObjectOutputStream` y después `ObjectInputStream` tanto en cliente como en servidor.

Esto lo hice para evitar bloqueos durante el establecimiento de conexión.

### Baneo gestionado en cliente

El servidor no mantiene una tabla global de baneos, porque el requisito no es impedir el envío, sino el no mostrar ciertos mensajes. 
Por eso el baneo se resuelve donde realmente tiene efecto: en el cliente.
