/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.bootstrap;

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

    /** Crea productos de tienda. */
    private void seedProducts() {
        if (productRepository.count() > 0) return;
        productRepository.saveAll(List.of(
            product("Camiseta local 24/25", "Camiseta blanquinegra ficticia de edición académica.", BigDecimal.valueOf(59.95), 40, "Textil"),
            product("Bufanda El Plantío", "Bufanda azul y blanca para día de partido.", BigDecimal.valueOf(17.50), 90, "Accesorios"),
            product("Taza Matchday", "Taza cerámica para cafeteros futboleros.", BigDecimal.valueOf(9.95), 60, "Hogar"),
            product("Pack día de partido", "Entrada simbólica, bufanda y póster con descuento.", BigDecimal.valueOf(34.95), 30, "Pack")
        ));
    }

    /** Crea producto reutilizable. */
    private Product product(String name, String description, BigDecimal price, int stock, String category) {
        return Product.builder().name(name).description(description).price(price).stock(stock).category(category).imageUrl("/images/product.svg").active(true).build();
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

    /** Crea FAQ del asistente. */
    private void seedFaqs() {
        if (faqRepository.count() > 0) return;
        faqRepository.saveAll(List.of(
            faq("¿Cómo compro una entrada?", "Entra en Partidos, abre el próximo encuentro, elige zona y confirma la pasarela simulada.", "Entradas"),
            faq("¿Dónde puedo aparcar?", "El visor cartográfico muestra parkings cercanos y su ocupación simulada mediante sensores.", "Mapa"),
            faq("¿Cómo veo mis pedidos?", "Tras iniciar sesión, abre Dashboard o Mis pedidos para consultar compras y referencias de pago.", "Usuario"),
            faq("¿Qué tecnologías usa la aplicación?", "Spring Boot, JPA, Thymeleaf, JWT, RabbitMQ, Redis, WebSockets, Flask, Leaflet, MySQL, Docker y SonarQube.", "Técnico")
        ));
    }

    /** Crea pregunta frecuente. */
    private Faq faq(String question, String answer, String category) {
        return Faq.builder().question(question).answer(answer).category(category).active(true).build();
    }
}
