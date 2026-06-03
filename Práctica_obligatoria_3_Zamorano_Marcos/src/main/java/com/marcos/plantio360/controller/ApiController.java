/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.CheckoutRequest;
import com.marcos.plantio360.model.*;
import com.marcos.plantio360.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API REST pública y privada para pruebas con Postman.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final CatalogService catalogService;
    private final CurrentUserService currentUserService;
    private final PurchaseService purchaseService;
    private final SensorService sensorService;

    /** Devuelve jugadores. */
    @GetMapping("/players")
    public List<Player> players() { return catalogService.findPlayers(); }

    /** Devuelve partidos. */
    @GetMapping("/matches")
    public List<FootballMatch> matches() { return catalogService.findUpcomingMatches(); }

    /** Devuelve productos activos. */
    @GetMapping("/products")
    public List<Product> products() { return catalogService.findProducts(); }

    /** Devuelve puntos cartográficos. */
    @GetMapping("/map-points")
    public List<MapPoint> mapPoints() { return catalogService.findMapPoints(); }

    /** Devuelve sensores recientes. */
    @GetMapping("/sensors/latest")
    public List<SensorReading> latestSensors() { return catalogService.findLatestSensors(); }

    /** Sincroniza sensores desde Flask. */
    @PostMapping("/sensors/sync")
    public List<SensorReading> syncSensors() { return sensorService.syncFromPythonApi(); }

    /** Ejecuta checkout autenticado. */
    @PostMapping("/checkout")
    public ResponseEntity<PlantioOrder> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(purchaseService.checkout(currentUserService.require(), request));
    }

    /** Devuelve pedidos del usuario autenticado. */
    @GetMapping("/me/orders")
    public List<PlantioOrder> myOrders() { return purchaseService.findOrders(currentUserService.require()); }

    /** Devuelve entradas del usuario autenticado. */
    @GetMapping("/me/tickets")
    public List<Ticket> myTickets() { return purchaseService.findTickets(currentUserService.require()); }
}
