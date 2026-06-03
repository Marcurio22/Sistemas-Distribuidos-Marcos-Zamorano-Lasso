/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.dto.CheckoutRequest;
import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de compra simulada de entradas y merchandising.
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final FootballMatchRepository matchRepository;
    private final ProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final PlantioOrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;

    /**
     * Compra una entrada y genera una orden de pago simulada.
     *
     * @param user usuario comprador.
     * @param matchId identificador del partido.
     * @param zone zona del estadio.
     * @return pedido creado.
     */
    @Transactional
    public PlantioOrder buyTicket(AppUser user, Long matchId, String zone) {
        FootballMatch match = matchRepository.findById(matchId).orElseThrow(() -> new IllegalArgumentException("Partido no encontrado"));
        if (match.getAvailableTickets() == null || match.getAvailableTickets() <= 0) {
            throw new IllegalStateException("No quedan entradas disponibles para este partido");
        }
        BigDecimal price = match.getBasePrice();
        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Ticket ticket = Ticket.builder()
            .match(match)
            .owner(user)
            .zone(zone == null || zone.isBlank() ? "Lateral" : zone)
            .seatCode("BFC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
            .price(price)
            .status("SOLD")
            .qrCode("QR-PLANTIO-" + UUID.randomUUID())
            .soldAt(LocalDateTime.now())
            .build();
        ticketRepository.save(ticket);
        match.setAvailableTickets(match.getAvailableTickets() - 1);
        matchRepository.save(match);

        PlantioOrder order = PlantioOrder.builder()
            .user(user)
            .status("PAID")
            .total(price)
            .paymentReference(reference)
            .createdAt(LocalDateTime.now())
            .items(new ArrayList<>())
            .build();
        order.addItem(OrderItem.builder().itemName("Entrada vs " + match.getRival()).quantity(1).unitPrice(price).build());
        PlantioOrder saved = orderRepository.save(order);
        rabbitTemplate.convertAndSend("plantio.purchase.created", "Compra de entrada: " + reference + " - " + user.getEmail());
        notificationService.notifyUser(user, "PURCHASE", "Entrada confirmada", "Tu entrada para el partido contra " + match.getRival() + " se ha generado correctamente.");
        return saved;
    }

    /**
     * Compra un producto de tienda con pago simulado.
     *
     * @param user usuario comprador.
     * @param productId identificador del producto.
     * @param quantity unidades.
     * @return pedido creado.
     */
    @Transactional
    public PlantioOrder buyProduct(AppUser user, Long productId, int quantity) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        int safeQuantity = Math.max(quantity, 1);
        int availableStock = product.getStock() == null ? 0 : product.getStock();
        if (availableStock <= 0) {
            throw new IllegalStateException("Este producto no tiene stock disponible ahora mismo.");
        }
        if (safeQuantity > availableStock) {
            throw new IllegalStateException("Solo quedan " + availableStock + " unidades disponibles. Ajusta la cantidad para continuar.");
        }
        product.setStock(product.getStock() - safeQuantity);
        productRepository.save(product);
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(safeQuantity));
        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PlantioOrder order = PlantioOrder.builder()
            .user(user)
            .status("PAID")
            .total(total)
            .paymentReference(reference)
            .createdAt(LocalDateTime.now())
            .items(new ArrayList<>())
            .build();
        order.addItem(OrderItem.builder().itemName(product.getName()).quantity(safeQuantity).unitPrice(product.getPrice()).build());
        PlantioOrder saved = orderRepository.save(order);
        rabbitTemplate.convertAndSend("plantio.purchase.created", "Compra en tienda: " + reference + " - " + user.getEmail());
        notificationService.notifyUser(user, "PURCHASE", "Pedido confirmado", "Tu pedido de " + product.getName() + " se ha confirmado correctamente.");
        return saved;
    }

    /**
     * Procesa un checkout genérico recibido por API REST.
     *
     * @param user usuario autenticado.
     * @param request petición de checkout.
     * @return pedido generado.
     */
    public PlantioOrder checkout(AppUser user, CheckoutRequest request) {
        if ("TICKET".equalsIgnoreCase(request.getType())) {
            return buyTicket(user, request.getItemId(), request.getZone());
        }
        return buyProduct(user, request.getItemId(), request.getQuantity());
    }

    /**
     * Recupera pedidos del usuario.
     *
     * @param user usuario autenticado.
     * @return pedidos recientes.
     */
    @Transactional(readOnly = true)
    public List<PlantioOrder> findOrders(AppUser user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Recupera entradas del usuario.
     *
     * @param user usuario autenticado.
     * @return entradas compradas.
     */
    @Transactional(readOnly = true)
    public List<Ticket> findTickets(AppUser user) {
        return ticketRepository.findByOwner(user);
    }
}
