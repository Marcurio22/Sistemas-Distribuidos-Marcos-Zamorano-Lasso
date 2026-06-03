/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import com.marcos.plantio360.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Controlador de administración para CRUD de entidades principales.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final PlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final ProductRepository productRepository;
    private final MapPointRepository mapPointRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final FaqRepository faqRepository;
    private final PlantioOrderRepository orderRepository;
    private final AssistantLogRepository assistantLogRepository;

    /** Muestra resumen de administración. */
    @GetMapping
    public String admin(Model model) {
        model.addAttribute("users", userService.findAll().size());
        model.addAttribute("players", playerRepository.count());
        model.addAttribute("matches", matchRepository.count());
        model.addAttribute("products", productRepository.count());
        model.addAttribute("orders", orderRepository.count());
        return "admin/index";
    }

    /** Lista usuarios. */
    @GetMapping("/users")
    public String users(Model model) { model.addAttribute("items", userService.findAll()); return "admin/users"; }

    /** Formulario de usuario. */
    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("item", AppUser.builder().role("ROLE_USER").enabled(true).build());
        return "admin/user-form";
    }

    /** Edición de usuario. */
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) { model.addAttribute("item", userService.findById(id)); return "admin/user-form"; }

    /** Guarda usuario. */
    @PostMapping("/users")
    public String saveUser(@ModelAttribute AppUser user, RedirectAttributes redirectAttributes) {
        userService.saveAdmin(user);
        redirectAttributes.addFlashAttribute("success", "Usuario guardado");
        return "redirect:/admin/users";
    }

    /** Elimina usuario. */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) { userService.delete(id); return "redirect:/admin/users"; }

    /** Lista jugadores. */
    @GetMapping("/players")
    public String players(Model model) { model.addAttribute("items", playerRepository.findAllByOrderByDorsalAsc()); return "admin/players"; }

    /** Nuevo jugador. */
    @GetMapping("/players/new")
    public String newPlayer(Model model) { model.addAttribute("item", Player.builder().status("DISPONIBLE").goals(0).assists(0).build()); return "admin/player-form"; }

    /** Edita jugador. */
    @GetMapping("/players/{id}/edit")
    public String editPlayer(@PathVariable Long id, Model model) { model.addAttribute("item", playerRepository.findById(id).orElseThrow()); return "admin/player-form"; }

    /** Guarda jugador. */
    @PostMapping("/players")
    public String savePlayer(@ModelAttribute Player player) { playerRepository.save(player); return "redirect:/admin/players"; }

    /** Elimina jugador. */
    @PostMapping("/players/{id}/delete")
    public String deletePlayer(@PathVariable Long id) { playerRepository.deleteById(id); return "redirect:/admin/players"; }

    /** Lista partidos. */
    @GetMapping("/matches")
    public String matches(Model model) { model.addAttribute("items", matchRepository.findAll()); return "admin/matches"; }

    /** Nuevo partido. */
    @GetMapping("/matches/new")
    public String newMatch(Model model) {
        model.addAttribute("item", FootballMatch.builder().matchDate(LocalDateTime.now().plusDays(7)).stadium("El Plantío").basePrice(BigDecimal.valueOf(25)).availableTickets(5000).status("PROGRAMADO").build());
        return "admin/match-form";
    }

    /** Edita partido. */
    @GetMapping("/matches/{id}/edit")
    public String editMatch(@PathVariable Long id, Model model) { model.addAttribute("item", matchRepository.findById(id).orElseThrow()); return "admin/match-form"; }

    /** Guarda partido. */
    @PostMapping("/matches")
    public String saveMatch(@ModelAttribute FootballMatch match) { matchRepository.save(match); return "redirect:/admin/matches"; }

    /** Elimina partido. */
    @PostMapping("/matches/{id}/delete")
    public String deleteMatch(@PathVariable Long id) { matchRepository.deleteById(id); return "redirect:/admin/matches"; }

    /** Lista productos. */
    @GetMapping("/products")
    public String products(Model model) { model.addAttribute("items", productRepository.findAll()); return "admin/products"; }

    /** Nuevo producto. */
    @GetMapping("/products/new")
    public String newProduct(Model model) { model.addAttribute("item", Product.builder().active(true).stock(25).price(BigDecimal.TEN).build()); return "admin/product-form"; }

    /** Edita producto. */
    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) { model.addAttribute("item", productRepository.findById(id).orElseThrow()); return "admin/product-form"; }

    /** Guarda producto. */
    @PostMapping("/products")
    public String saveProduct(@ModelAttribute Product product) { productRepository.save(product); return "redirect:/admin/products"; }

    /** Elimina producto. */
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) { productRepository.deleteById(id); return "redirect:/admin/products"; }

    /** Lista puntos del mapa. */
    @GetMapping("/map-points")
    public String mapPoints(Model model) { model.addAttribute("items", mapPointRepository.findAll()); return "admin/map-points"; }

    /** Nuevo punto del mapa. */
    @GetMapping("/map-points/new")
    public String newMapPoint(Model model) { model.addAttribute("item", MapPoint.builder().type("PARKING").latitude(BigDecimal.valueOf(42.3501)).longitude(BigDecimal.valueOf(-3.6892)).build()); return "admin/map-point-form"; }

    /** Edita punto del mapa. */
    @GetMapping("/map-points/{id}/edit")
    public String editMapPoint(@PathVariable Long id, Model model) { model.addAttribute("item", mapPointRepository.findById(id).orElseThrow()); return "admin/map-point-form"; }

    /** Guarda punto del mapa. */
    @PostMapping("/map-points")
    public String saveMapPoint(@ModelAttribute MapPoint point) { mapPointRepository.save(point); return "redirect:/admin/map-points"; }

    /** Elimina punto del mapa. */
    @PostMapping("/map-points/{id}/delete")
    public String deleteMapPoint(@PathVariable Long id) { mapPointRepository.deleteById(id); return "redirect:/admin/map-points"; }

    /** Lista sensores. */
    @GetMapping("/sensors")
    public String sensors(Model model) { model.addAttribute("items", sensorReadingRepository.findTop20ByOrderByCapturedAtDesc()); return "admin/sensors"; }

    /** Nuevo sensor. */
    @GetMapping("/sensors/new")
    public String newSensor(Model model) { model.addAttribute("item", SensorReading.builder().type("PARKING").status("NORMAL").value(BigDecimal.ZERO).capturedAt(LocalDateTime.now()).build()); return "admin/sensor-form"; }

    /** Guarda sensor. */
    @PostMapping("/sensors")
    public String saveSensor(@ModelAttribute SensorReading sensor) { sensorReadingRepository.save(sensor); return "redirect:/admin/sensors"; }

    /** Lista FAQs. */
    @GetMapping("/faqs")
    public String faqs(Model model) { model.addAttribute("items", faqRepository.findAll()); return "admin/faqs"; }

    /** Nueva FAQ. */
    @GetMapping("/faqs/new")
    public String newFaq(Model model) { model.addAttribute("item", Faq.builder().active(true).category("General").build()); return "admin/faq-form"; }

    /** Edita FAQ. */
    @GetMapping("/faqs/{id}/edit")
    public String editFaq(@PathVariable Long id, Model model) { model.addAttribute("item", faqRepository.findById(id).orElseThrow()); return "admin/faq-form"; }

    /** Guarda FAQ. */
    @PostMapping("/faqs")
    public String saveFaq(@ModelAttribute Faq faq) { faqRepository.save(faq); return "redirect:/admin/faqs"; }

    /** Elimina FAQ. */
    @PostMapping("/faqs/{id}/delete")
    public String deleteFaq(@PathVariable Long id) { faqRepository.deleteById(id); return "redirect:/admin/faqs"; }

    /** Lista pedidos. */
    @GetMapping("/orders")
    public String orders(Model model) { model.addAttribute("items", orderRepository.findAll()); return "admin/orders"; }

    /** Lista logs del asistente. */
    @GetMapping("/assistant-logs")
    public String assistantLogs(Model model) { model.addAttribute("items", assistantLogRepository.findAll()); return "admin/assistant-logs"; }
}
