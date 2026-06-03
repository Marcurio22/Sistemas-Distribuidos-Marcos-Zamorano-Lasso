/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Servicio de consulta de catálogos públicos: jugadores, partidos, productos, mapa y sensores.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {
    private final PlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final ProductRepository productRepository;
    private final MapPointRepository mapPointRepository;
    private final SensorReadingRepository sensorReadingRepository;

    /**
     * Devuelve la plantilla ordenada por dorsal.
     *
     * @return lista de jugadores.
     */
    @Cacheable(value = "players", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Player> findPlayers() {
        return playerRepository.findAllByOrderByDorsalAsc();
    }

    /**
     * Devuelve un jugador por identificador.
     *
     * @param id identificador del jugador.
     * @return jugador encontrado.
     */
    public Player findPlayer(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));
    }

    /**
     * Devuelve partidos futuros.
     *
     * @return lista de partidos próximos.
     */
    @Cacheable(value = "matches", key = "'upcoming'", unless = "#result == null || #result.isEmpty()")
    public List<FootballMatch> findUpcomingMatches() {
        return matchRepository.findByMatchDateAfterOrderByMatchDateAsc(LocalDateTime.now().minusDays(1));
    }

    /**
     * Devuelve un partido por identificador.
     *
     * @param id identificador del partido.
     * @return partido encontrado.
     */
    public FootballMatch findMatch(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
    }

    /**
     * Devuelve productos activos ordenados de forma comercial.
     * Las tres equipaciones aparecen juntas para facilitar la comparación visual.
     * No se cachea porque el stock cambia tras cada compra simulada.
     *
     * @return lista de productos de tienda.
     */
    public List<Product> findProducts() {
        Map<String, Integer> priority = Map.of(
            "Camiseta local 25/26", 1,
            "Segunda equipación 25/26", 2,
            "Tercera equipación 25/26", 3,
            "Bufanda El Plantío", 4,
            "Taza Matchday", 5,
            "Pack día de partido", 6
        );
        List<Product> products = new ArrayList<>(productRepository.findByActiveTrue());
        products.sort(Comparator
            .comparing((Product product) -> priority.getOrDefault(product.getName(), 100))
            .thenComparing(Product::getName));
        return products;
    }

    /**
     * Devuelve un producto por identificador.
     *
     * @param id identificador del producto.
     * @return producto encontrado.
     */
    public Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    /**
     * Devuelve todos los puntos del mapa.
     *
     * @return puntos cartográficos.
     */
    public List<MapPoint> findMapPoints() {
        return mapPointRepository.findAll();
    }

    /**
     * Devuelve las últimas lecturas de sensores.
     *
     * @return lecturas recientes.
     */
    @Cacheable(value = "sensors", key = "'latest'", unless = "#result == null || #result.isEmpty()")
    public List<SensorReading> findLatestSensors() {
        return sensorReadingRepository.findTop20ByOrderByCapturedAtDesc();
    }
}
