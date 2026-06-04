/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.CheckoutRequest;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de páginas públicas y privadas de usuario.
 */
@Controller
@RequiredArgsConstructor
public class PageController {
    private final CatalogService catalogService;
    private final CurrentUserService currentUserService;
    private final PurchaseService purchaseService;
    private final NotificationService notificationService;
    private final SensorService sensorService;

    /**
     * Renderiza la página principal.
     *
     * @param model modelo de vista.
     * @return home.
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("players", catalogService.findPlayers().stream()
            .filter(player -> java.util.List.of("Ander Cantero", "Álex Lizancos", "Curro", "Fer Niño").contains(player.getName()))
            .sorted(java.util.Comparator.comparingInt(player -> java.util.List.of("Ander Cantero", "Álex Lizancos", "Curro", "Fer Niño").indexOf(player.getName())))
            .toList());
        model.addAttribute("matches", catalogService.findUpcomingMatches().stream().limit(3).toList());
        model.addAttribute("products", catalogService.findProducts().stream().limit(3).toList());
        return "home";
    }

    /** Muestra la plantilla completa. */
    @GetMapping("/players")
    public String players(Model model) {
        model.addAttribute("players", catalogService.findPlayers());
        return "catalog/players";
    }

    /** Muestra detalle de jugador. */
    @GetMapping("/players/{id}")
    public String player(@PathVariable Long id, Model model) {
        model.addAttribute("player", catalogService.findPlayer(id));
        return "catalog/player-detail";
    }

    /** Muestra partidos próximos. */
    @GetMapping("/matches")
    public String matches(Model model) {
        model.addAttribute("matches", catalogService.findUpcomingMatches());
        return "catalog/matches";
    }

    /** Muestra detalle de partido y formulario de compra. */
    @GetMapping("/matches/{id}")
    public String match(@PathVariable Long id, Model model) {
        model.addAttribute("match", catalogService.findMatch(id));
        model.addAttribute("checkout", new CheckoutRequest());
        return "catalog/match-detail";
    }

    /** Muestra tienda de merchandising. */
    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute("products", catalogService.findProducts());
        return "catalog/shop";
    }

    /** Muestra detalle de producto y formulario de compra. */
    @GetMapping("/shop/{id}")
    public String product(@PathVariable Long id, Model model) {
        model.addAttribute("product", catalogService.findProduct(id));
        model.addAttribute("checkout", new CheckoutRequest());
        return "catalog/product-detail";
    }

    /** Renderiza el visor Leaflet sin API keys. */
    @GetMapping("/map")
    public String map(Model model) {
        model.addAttribute("points", catalogService.findMapPoints());
        model.addAttribute("sensors", catalogService.findLatestSensors());
        return "map";
    }

    /** Muestra panel privado del usuario. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AppUser user = currentUserService.require();
        model.addAttribute("user", user);
        model.addAttribute("orders", purchaseService.findOrders(user).stream().limit(5).toList());
        model.addAttribute("notifications", notificationService.findForUser(user).stream().limit(5).toList());
        return "dashboard";
    }

    /** Muestra perfil del usuario. */
    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("user", currentUserService.require());
        return "profile";
    }

    /** Muestra pedidos del usuario. */
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("orders", purchaseService.findOrders(currentUserService.require()));
        return "orders";
    }

    /** Muestra entradas del usuario. */
    @GetMapping("/my-tickets")
    public String tickets(Model model) {
        model.addAttribute("tickets", purchaseService.findTickets(currentUserService.require()));
        return "tickets";
    }

    /** Ejecuta compra de entrada. */
    @PostMapping("/checkout/ticket/{matchId}")
    public String buyTicket(@PathVariable Long matchId, @ModelAttribute CheckoutRequest request, RedirectAttributes redirectAttributes) {
        purchaseService.buyTicket(currentUserService.require(), matchId, request.getZone());
        redirectAttributes.addFlashAttribute("success", "Entrada comprada correctamente. Revisa tus entradas y MailHog.");
        return "redirect:/my-tickets";
    }

    /** Ejecuta compra de producto y devuelve errores controlados si no hay stock suficiente. */
    @PostMapping("/checkout/product/{productId}")
    public String buyProduct(@PathVariable Long productId, @ModelAttribute CheckoutRequest request, RedirectAttributes redirectAttributes) {
        try {
            purchaseService.buyProduct(currentUserService.require(), productId, request.getQuantity() == null ? 1 : request.getQuantity());
            redirectAttributes.addFlashAttribute("success", "Pedido confirmado correctamente. Correo simulado disponible en MailHog.");
            return "redirect:/orders";
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/shop/" + productId;
        }
    }

    /** Fuerza sincronización manual de sensores desde Flask. */
    @PostMapping("/sensors/sync")
    public String syncSensors(RedirectAttributes redirectAttributes) {
        sensorService.syncFromPythonApi();
        redirectAttributes.addFlashAttribute("success", "Sensores sincronizados desde Flask.");
        return "redirect:/map";
    }
}
