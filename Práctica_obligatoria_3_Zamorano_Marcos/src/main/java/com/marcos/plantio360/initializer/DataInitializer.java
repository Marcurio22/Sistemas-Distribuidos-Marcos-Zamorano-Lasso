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

    /** Crea jugadores ficticios. */
    private void seedPlayers() {
        if (playerRepository.count() > 0) return;
        playerRepository.saveAll(List.of(
            player("Álex Sancristóbal", 1, "Portero", "España", 27, "Guardameta seguro bajo palos y líder defensivo."),
            player("Diego Burgalés", 4, "Defensa", "España", 25, "Central contundente, fuerte por arriba y fiable en salida."),
            player("Hugo Arlanzón", 8, "Mediocentro", "España", 24, "Organizador con buena lectura del juego y balón parado."),
            player("Mateo Plantío", 10, "Mediapunta", "Argentina", 23, "Jugador creativo, vertical y determinante entre líneas."),
            player("Iker Gamonal", 9, "Delantero", "España", 26, "Referencia ofensiva con buen remate y presión alta."),
            player("Lucas Catedral", 11, "Extremo", "España", 22, "Extremo rápido para atacar espacios y asistir desde banda.")
        ));
    }

    /** Crea una entidad jugador reutilizable. */
    private Player player(String name, int dorsal, String position, String nationality, int age, String description) {
        return Player.builder().name(name).dorsal(dorsal).position(position).nationality(nationality).age(age)
            .height(BigDecimal.valueOf(1.82)).weight(BigDecimal.valueOf(76)).imageUrl("/images/player.svg")
            .description(description).status("DISPONIBLE").goals(dorsal % 5).assists(dorsal % 3).build();
    }

    /** Crea partidos iniciales. */
    private void seedMatches() {
        if (matchRepository.count() > 0) return;
        matchRepository.saveAll(List.of(
            match("Real Oviedo", "LaLiga Hypermotion", 7, BigDecimal.valueOf(22), 7300),
            match("Racing de Santander", "LaLiga Hypermotion", 15, BigDecimal.valueOf(28), 6200),
            match("Real Zaragoza", "Copa del Rey", 28, BigDecimal.valueOf(18), 8100)
        ));
    }

    /** Crea un partido reutilizable. */
    private FootballMatch match(String rival, String competition, int days, BigDecimal price, int tickets) {
        return FootballMatch.builder().rival(rival).competition(competition).matchDate(LocalDateTime.now().plusDays(days))
            .stadium("El Plantío").basePrice(price).availableTickets(tickets).status("PROGRAMADO").imageUrl("/images/stadium.svg").build();
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
        upsertFaq("¿Cómo veo mis pedidos?", "Tras iniciar sesión, abre Dashboard o Mis pedidos para consultar compras y referencias de pago.", "Usuario");
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
