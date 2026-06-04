/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.initializer;

import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carga datos iniciales para que el profesor no tenga que configurar nada manualmente.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final ProductRepository productRepository;
    private final MapPointRepository mapPointRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final FaqRepository faqRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Ejecuta la carga idempotente de usuarios, catálogo, mapa, sensores y FAQ.
     *
     * @param args argumentos de arranque.
     */
    @Override
    public void run(String... args) {
        seedUsers();
        seedPlayers();
        seedMatches();
        seedProducts();
        seedMap();
        seedSensors();
        seedFaqs();
    }

    /** Crea usuarios demo. */
    private void seedUsers() {
        if (userRepository.existsByEmail("admin@plantio360.local")) return;
        userRepository.save(AppUser.builder().firstName("Marcos").lastName("Zamorano Lasso").email("admin@plantio360.local").password(passwordEncoder.encode("admin1234")).role("ROLE_ADMIN").phone("600000000").enabled(true).avatarUrl("/images/avatar.svg").build());
        userRepository.save(AppUser.builder().firstName("Aficionado").lastName("Blanquinegro").email("user@plantio360.local").password(passwordEncoder.encode("user1234")).role("ROLE_USER").phone("611111111").enabled(true).avatarUrl("/images/avatar.svg").build());
    }

    /** Crea o actualiza la plantilla oficial usada como base visual del prototipo. */
    private void seedPlayers() {
        List<Player> officialPlayers = List.of(
            player("Jesús Ruiz", 1, "Portero", "España", 29, 1.85, null, "DISPONIBLE", 0, 0, "Guardameta sobrio para completar la portería blanquinegra con seguridad bajo palos.", "/images/players/jesus-ruiz.png"),
            player("Álex Lizancos", 2, "Defensa", "España", 22, 1.86, null, "DISPONIBLE", 1, 4, "Lateral joven, intenso y con recorrido para sumar profundidad por la banda.", "/images/players/alex-lizancos.png"),
            player("Oier Luengo", 3, "Defensa", "España", 28, 1.85, null, "CEDIDO", 0, 0, "Central competitivo, contundente en duelos y útil para reforzar la zaga.", "/images/players/oier-luengo.png"),
            player("Pablo Galdames", 4, "Centrocampista", "Chile", 29, null, null, "DISPONIBLE", 1, 2, "Mediocentro chileno de trabajo, equilibrio y buena lectura táctica.", "/images/players/pablo-galdames.png"),
            player("Atienza", 5, "Centrocampista", "España", 27, 1.86, 77, "SANCIONADO", 2, 1, "Pivote de jerarquía para ordenar al equipo y ganar disputas en la medular.", "/images/players/atienza.png"),
            player("Sergio González", 6, "Defensa", "España", 29, 1.84, null, "DISPONIBLE", 0, 1, "Defensa fiable, ambidiestro y con experiencia para sostener el bloque.", "/images/players/sergio-gonzalez.png"),
            player("Mateo Mejía", 7, "Delantero", "España", 23, 1.81, null, "DISPONIBLE", 3, 1, "Atacante rápido para romper líneas, atacar espacios y presionar arriba.", "/images/players/mateo-mejia.png"),
            player("Grego Sierra", 8, "Defensa", "España", 33, 1.86, 74, "LESIONADO", 2, 1, "Defensor zurdo con carácter, experiencia y dominio del juego aéreo.", "/images/players/grego-sierra.png"),
            player("Fer Niño", 9, "Delantero", "España", 25, 1.91, 78, "DISPONIBLE", 7, 2, "Delantero centro de referencia, poderoso en el área y dominante por arriba.", "/images/players/fer-nino.png"),
            player("Appin", 10, "Centrocampista", "Francia", 28, 1.77, 71, "DISPONIBLE", 4, 2, "Centrocampista físico y vertical para acelerar transiciones y recuperar balones.", "/images/players/appin.png"),
            player("Víctor Mollejo", 11, "Delantero", "España", 25, 1.77, null, "DUDA", 1, 3, "Atacante eléctrico, intenso y muy útil entre líneas y en banda.", "/images/players/victor-mollejo.png"),
            player("Florian Miguel", 12, "Defensa", "Francia", 29, 1.79, null, "DISPONIBLE", 2, 3, "Lateral zurdo polivalente, profundo y sólido en fase defensiva.", "/images/players/florian-miguel.png"),
            player("Ander Cantero", 13, "Portero", "España", 31, null, null, "DISPONIBLE", 0, 0, "Portero experimentado, líder desde atrás y especialista en mantener la portería a cero.", "/images/players/ander-cantero.png"),
            player("David González", 14, "Centrocampista", "España", 23, 1.80, null, "DISPONIBLE", 12, 6, "Talento burgalés, llegada, balón parado y mucha calidad para decidir partidos.", "/images/players/david-gonzalez.png"),
            player("Aitor Buñuel", 15, "Defensa", "España", 28, 1.70, null, "DUDA", 0, 1, "Lateral derecho intenso, disciplinado y con recorrido para doblar por fuera.", "/images/players/aitor-bunuel.png"),
            player("Curro", 16, "Centrocampista", "España", 30, 1.74, 71, "FENOMENAL", 18, 14, "Fenomenal en todo: el mejor del equipo, líder técnico, zurda diferencial, visión de juego y máximo impacto ofensivo.", "/images/players/curro-sanchez.png"),
            player("Mario Cantero", 17, "Centrocampista", "España", 24, null, null, "DISPONIBLE", 1, 2, "Centrocampista joven, dinámico y con margen para crecer en el primer equipo.", "/images/players/mario-cantero.png"),
            player("Mario González", 20, "Delantero", "España", 30, 1.84, null, "DISPONIBLE", 4, 1, "Delantero burgalés con movilidad, experiencia y olfato para atacar el área.", "/images/players/mario-gonzalez.png"),
            player("Iñigo Córdoba", 21, "Delantero", "España", 29, 1.77, null, "DISPONIBLE", 2, 4, "Atacante zurdo con buen regate, último pase y capacidad para romper por banda.", "/images/players/inigo-cordoba.png"),
            player("Brais Martínez", 22, "Defensa", "España", 24, 1.75, null, "NO DISPONIBLE", 0, 0, "Defensa zurdo de proyección, actualmente marcado como no disponible para mostrar el estado en la interfaz.", "/images/players/brais-martinez.png"),
            player("Iván Morante", 23, "Centrocampista", "España", 25, null, null, "DISPONIBLE", 2, 4, "Mediocentro de control, precisión en el pase y capacidad para gobernar el ritmo.", "/images/players/ivan-morante.png"),
            player("Saúl del Cerro", 28, "Centrocampista", "España", 22, null, null, "DISPONIBLE", 0, 1, "Canterano burgalés, trabajador y competitivo para aportar energía a la medular.", "/images/players/saul-del-cerro.png"),
            player("Hugo Cuenca", 30, "Delantero", "Paraguay", 21, null, null, "DISPONIBLE", 2, 2, "Delantero paraguayo joven, zurdo y con margen para aportar desequilibrio ofensivo.", "/images/players/hugo-cuenca.png"),
            player("Marcelo Expósito", 33, "Centrocampista", "España", 23, 1.81, null, "DISPONIBLE", 0, 1, "Centrocampista con buen manejo y presencia para sumar alternativas en la rotación.", "/images/players/marcelo-exposito.png")
        );

        Set<String> officialNames = officialPlayers.stream().map(Player::getName).collect(Collectors.toSet());
        playerRepository.findAll().stream()
            .filter(existing -> !officialNames.contains(existing.getName()))
            .forEach(playerRepository::delete);
        officialPlayers.forEach(this::upsertPlayer);
    }

    /**
     * Crea una entidad jugador reutilizable.
     *
     * @param name nombre deportivo.
     * @param dorsal dorsal del jugador.
     * @param position posición principal.
     * @param nationality nacionalidad.
     * @param age edad deportiva de referencia.
     * @param heightMeters altura en metros, si está disponible.
     * @param weightKg peso en kilogramos, si está disponible.
     * @param status estado mostrado en el frontend.
     * @param goals goles simulados o adaptados para el prototipo.
     * @param assists asistencias simuladas o adaptadas para el prototipo.
     * @param description descripción visual para la ficha.
     * @param imageUrl imagen o tarjeta visual del jugador.
     * @return jugador preparado para persistencia.
     */
    private Player player(String name, int dorsal, String position, String nationality, int age, Double heightMeters, Integer weightKg, String status, int goals, int assists, String description, String imageUrl) {
        return Player.builder()
            .name(name)
            .dorsal(dorsal)
            .position(position)
            .nationality(nationality)
            .age(age)
            .height(heightMeters == null ? null : BigDecimal.valueOf(heightMeters))
            .weight(weightKg == null ? null : BigDecimal.valueOf(weightKg))
            .imageUrl(imageUrl)
            .description(description)
            .status(status)
            .goals(goals)
            .assists(assists)
            .build();
    }

    /**
     * Crea o actualiza un jugador por nombre o dorsal para sustituir datos ficticios previos.
     *
     * @param seed jugador con los datos actualizados.
     */
    private void upsertPlayer(Player seed) {
        Player player = playerRepository.findAll().stream()
            .filter(existing -> existing.getName().equals(seed.getName()) || existing.getDorsal().equals(seed.getDorsal()))
            .findFirst()
            .orElseGet(Player::new);
        player.setName(seed.getName());
        player.setDorsal(seed.getDorsal());
        player.setPosition(seed.getPosition());
        player.setNationality(seed.getNationality());
        player.setAge(seed.getAge());
        player.setHeight(seed.getHeight());
        player.setWeight(seed.getWeight());
        player.setImageUrl(seed.getImageUrl());
        player.setDescription(seed.getDescription());
        player.setStatus(seed.getStatus());
        player.setGoals(seed.getGoals());
        player.setAssists(seed.getAssists());
        playerRepository.save(player);
    }

    /** Crea o actualiza partidos iniciales de forma idempotente y siempre futuros. */
    private void seedMatches() {
        upsertMatch("Real Oviedo", "LaLiga Hypermotion", 7, BigDecimal.valueOf(22), 7300, false, "/images/team-default-shield.svg");
        upsertMatch("Racing de Santander", "LaLiga Hypermotion", 15, BigDecimal.valueOf(28), 6200, true, "/images/team-default-shield.svg");
        upsertMatch("Real Zaragoza", "Copa del Rey", 28, BigDecimal.valueOf(18), 8100, true, "/images/team-default-shield.svg");
    }

    /**
     * Crea o actualiza un partido para evitar datos antiguos tras reiniciar Docker.
     *
     * @param rival equipo rival.
     * @param competition competición oficial simulada.
     * @param days días desde el arranque.
     * @param price precio base.
     * @param tickets entradas iniciales.
     * @param homeGame indica si el Burgos CF juega como local.
     * @param imageUrl escudo del rival o placeholder.
     */
    private void upsertMatch(String rival, String competition, int days, BigDecimal price, int tickets, boolean homeGame, String imageUrl) {
        FootballMatch match = matchRepository.findAll().stream()
            .filter(existing -> existing.getRival().equalsIgnoreCase(rival))
            .findFirst()
            .orElseGet(FootballMatch::new);
        match.setRival(rival);
        match.setCompetition(competition);
        match.setMatchDate(LocalDateTime.now().plusDays(days));
        match.setStadium(homeGame ? "El Plantío" : "Estadio rival");
        match.setBasePrice(price);
        if (match.getAvailableTickets() == null || match.getAvailableTickets() <= 0) {
            match.setAvailableTickets(tickets);
        }
        match.setStatus("PROGRAMADO");
        match.setHomeGame(homeGame);
        if (match.getImageUrl() == null || match.getImageUrl().isBlank() || match.getImageUrl().equals("/images/stadium.svg")) {
            match.setImageUrl(imageUrl);
        }
        matchRepository.save(match);
    }

    /** Crea o actualiza productos de tienda de forma idempotente. */
    private void seedProducts() {
        upsertProduct("Camiseta local 25/26", "Camiseta local 24/25", "Camiseta blanquinegra para vivir los partidos al máximo!", BigDecimal.valueOf(59.95), 40, "Textil", "/images/camiseta-local-25-26.png");
        upsertProduct("Segunda equipación 25/26", null, "Camiseta negra de visitante con identidad blanquinegra para animar fuera de casa.", BigDecimal.valueOf(59.95), 18, "Textil", "/images/camiseta-segunda-25-26.png");
        upsertProduct("Tercera equipación 25/26", null, "Camiseta alternativa roja para los días más especiales de la temporada.", BigDecimal.valueOf(59.95), 11, "Textil", "/images/camiseta-tercera-25-26.png");
        upsertProduct("Bufanda El Plantío", null, "Bufanda negra y blanca para día de partido.", BigDecimal.valueOf(17.50), 90, "Accesorios", "/images/bufanda-burgos-cf.png");
        upsertProduct("Taza Matchday", null, "Taza de cerámica para cafeteros futboleros.", BigDecimal.valueOf(9.95), 60, "Hogar", "/images/taza-burgos-cf.png");
        upsertProduct("Pack día de partido", null, "Entrada simbólica, bufanda y póster con descuento para vivir una jornada completa en El Plantío.", BigDecimal.valueOf(34.95), 24, "Pack", "/images/pack-dia-partido.png");
    }

    /**
     * Crea o actualiza un producto conservando la base de datos existente.
     *
     * @param name nombre actual del producto.
     * @param legacyName nombre anterior del producto, si procede.
     * @param description descripción comercial.
     * @param price precio de venta.
     * @param stock unidades disponibles.
     * @param category categoría de tienda.
     * @param imageUrl imagen pública del producto.
     */
    private void upsertProduct(String name, String legacyName, String description, BigDecimal price, int stock, String category, String imageUrl) {
        Product product = productRepository.findByName(name)
            .or(() -> legacyName == null ? java.util.Optional.empty() : productRepository.findByName(legacyName))
            .orElseGet(Product::new);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        product.setImageUrl(imageUrl);
        product.setActive(true);
        productRepository.save(product);
    }

    /** Crea puntos del visor cartográfico. */
    private void seedMap() {
        if (mapPointRepository.count() > 0) return;
        mapPointRepository.saveAll(List.of(
            point("Estadio El Plantío", "ESTADIO", 42.3500, -3.6890, "Centro del visor cartográfico."),
            point("Parking Norte", "PARKING", 42.3512, -3.6902, "Aparcamiento principal para abonados."),
            point("Puerta 3", "PUERTA", 42.3497, -3.6883, "Acceso recomendado para tribuna lateral."),
            point("Zona visitante", "SEGURIDAD", 42.3504, -3.6876, "Sector reservado a afición visitante."),
            point("Peña Blanquinegra", "PENA", 42.3489, -3.6906, "Punto de encuentro previo al partido."),
            point("Bar Matchday", "BAR", 42.3490, -3.6914, "Zona de restauración cercana al estadio.")
        ));
    }

    /** Crea punto cartográfico. */
    private MapPoint point(String name, String type, double lat, double lon, String description) {
        return MapPoint.builder().name(name).type(type).latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lon)).description(description).build();
    }

    /** Crea sensores simulados. */
    private void seedSensors() {
        if (sensorReadingRepository.count() > 0) return;
        sensorReadingRepository.saveAll(List.of(
            sensor("Parking Norte", "PARKING", 72, "%", "WARNING", 42.3512, -3.6902),
            sensor("Puerta 3", "AFLUENCIA", 48, "%", "NORMAL", 42.3497, -3.6883),
            sensor("Césped", "HUMEDAD", 36, "%", "NORMAL", 42.3500, -3.6890),
            sensor("Temperatura estadio", "TEMPERATURA", 13, "ºC", "NORMAL", 42.3500, -3.6890)
        ));
    }

    /** Crea lectura de sensor. */
    private SensorReading sensor(String name, String type, double value, String unit, String status, double lat, double lon) {
        return SensorReading.builder().name(name).type(type).value(BigDecimal.valueOf(value)).unit(unit).status(status)
            .latitude(BigDecimal.valueOf(lat)).longitude(BigDecimal.valueOf(lon)).capturedAt(LocalDateTime.now()).build();
    }

    /** Crea o actualiza FAQs del asistente. */
    private void seedFaqs() {
        upsertFaq("¿Cómo compro una entrada?", "Entra en Partidos, abre el próximo encuentro, elige zona y confirma la pasarela simulada.", "Entradas");
        upsertFaq("¿Dónde puedo aparcar?", "El visor cartográfico muestra parkings cercanos y su ocupación simulada mediante sensores.", "Mapa");
        upsertFaq("¿Cómo veo mis pedidos?", "Tras iniciar sesión, abre Mi espacio o Mis pedidos para consultar compras y referencias de pago.", "Usuario");
        upsertFaq("¿Qué tecnologías usa la aplicación?", "Spring Boot, JPA, Thymeleaf, JWT, RabbitMQ, Redis, WebSockets, Flask, Leaflet, MySQL, Docker, SonarQube, Tailwind CSS y DaisyUI.", "Técnico");
        upsertFaq("¿Cómo funciona el login?", "La autenticación se realiza con JWT almacenado en cookie HttpOnly, por lo que las páginas privadas solo se muestran a usuarios autenticados.", "Seguridad");
        upsertFaq("¿Cómo cierro sesión correctamente?", "Pulsa Logout en la barra superior. La aplicación elimina la cookie JWT y redirige al login.", "Seguridad");
        upsertFaq("¿Qué puedo hacer como administrador?", "El rol administrador puede mantener usuarios, jugadores, partidos, productos, puntos del mapa, sensores, FAQs, pedidos y logs del asistente.", "Administración");
        upsertFaq("¿Cómo funciona la tienda?", "La tienda permite comprar productos ficticios con stock controlado. Al confirmar el pago se registra un pedido y se lanza un evento asíncrono.", "Tienda");
        upsertFaq("¿Qué es MailHog?", "MailHog es una bandeja de correo local para ver los emails simulados de la aplicación sin configurar Gmail ni un SMTP real.", "Email");
        upsertFaq("¿Qué papel tiene RabbitMQ?", "RabbitMQ procesa eventos de compra de forma asíncrona para simular un flujo distribuido desacoplado entre compra y notificación por email.", "Mensajería");
        upsertFaq("¿Para qué se usa Redis?", "Redis se usa como caché para mejorar el rendimiento de datos consultados con frecuencia, como recursos de catálogo o consultas auxiliares.", "Caché");
        upsertFaq("¿Qué hace el microservicio Flask?", "Flask expone una API REST de sensores simulados que Spring consume para alimentar el visor cartográfico y demostrar integración entre servicios.", "Flask");
        upsertFaq("¿El mapa usa Google Maps?", "No. El visor cartográfico usa Leaflet y OpenStreetMap, por lo que no necesita API keys.", "Mapa");
        upsertFaq("¿Cómo se sincronizan los sensores?", "Desde el visor puedes sincronizar sensores. Spring llama a Flask, recibe lecturas simuladas y las guarda en MySQL.", "Sensores");
        upsertFaq("¿Qué es el Muro Blanquinegro?", "Es un chat en tiempo real basado en WebSockets, SockJS y STOMP para simular interacción entre aficionados conectados.", "WebSockets");
    }

    /**
     * Crea o actualiza una FAQ por pregunta para mantener la carga idempotente.
     *
     * @param question pregunta frecuente.
     * @param answer respuesta mostrada al usuario.
     * @param category categoría funcional.
     */
    private void upsertFaq(String question, String answer, String category) {
        Faq faq = faqRepository.findByQuestion(question).orElseGet(Faq::new);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setCategory(category);
        faq.setActive(true);
        faqRepository.save(faq);
    }

    /** Crea pregunta frecuente. */
    private Faq faq(String question, String answer, String category) {
        return Faq.builder().question(question).answer(answer).category(category).active(true).build();
    }
}
