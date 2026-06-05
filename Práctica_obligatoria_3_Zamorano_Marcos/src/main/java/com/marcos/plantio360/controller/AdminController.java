/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import com.marcos.plantio360.service.AdminExportService;
import com.marcos.plantio360.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Controlador de administración para CRUD de entidades principales y exportaciones.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final long MAX_IMAGE_BYTES = 64L * 1024L * 1024L;
    private static final List<String> COMPETITIONS = List.of("LaLiga Hypermotion", "Copa del Rey");
    private static final List<String> PLAYER_POSITIONS = List.of("Portero", "Defensa", "Centrocampista", "Delantero");
    private static final List<String> PLAYER_STATUSES = List.of("DISPONIBLE", "FENOMENAL", "LESIONADO", "SANCIONADO", "DUDA", "CEDIDO", "NO DISPONIBLE");
    private static final List<String> PRODUCT_CATEGORIES = List.of("Textil", "Accesorios", "Hogar", "Pack", "Entradas", "Otros");
    private static final List<String> MAP_POINT_TYPES = List.of("ESTADIO", "PARKING", "PUERTA", "BAR", "TIENDA", "PEÑA", "PARADA_BUS", "SEGURIDAD", "SERVICIOS", "OTRO");
    private static final List<String> SENSOR_TYPES = List.of("PARKING", "AFLUENCIA", "TEMPERATURA", "HUMEDAD", "CÉSPED", "SEGURIDAD", "OTRO");
    private static final List<String> SENSOR_STATUSES = List.of("NORMAL", "WARNING", "CRITICAL");
    private static final List<String> FAQ_CATEGORIES = List.of("General", "Entradas", "Tienda", "Mapa", "Sensores", "Usuario", "Seguridad", "Administración", "Email", "Mensajería", "Caché", "Flask", "WebSockets", "Técnico");

    private final UserService userService;
    private final PlayerRepository playerRepository;
    private final FootballMatchRepository matchRepository;
    private final ProductRepository productRepository;
    private final MapPointRepository mapPointRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final FaqRepository faqRepository;
    private final PlantioOrderRepository orderRepository;
    private final AssistantLogRepository assistantLogRepository;
    private final AdminExportService adminExportService;

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
    public String users(Model model) {
        model.addAttribute("items", userService.findAll());
        return "admin/users";
    }

    /** Formulario de usuario. */
    @GetMapping("/users/new")
    public String newUser(Model model) {
        model.addAttribute("item", AppUser.builder().role("ROLE_USER").enabled(true).avatarUrl("/images/avatar.svg").build());
        return "admin/user-form";
    }

    /** Edición de usuario. */
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("item", userService.findById(id));
        return "admin/user-form";
    }

    /** Guarda usuario sin permitir cambios de contraseña desde administración. */
    @PostMapping("/users")
    public String saveUser(@ModelAttribute("item") AppUser user, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) return userFormError(model, user, bindingMessage("usuario", bindingResult));
            userService.saveAdmin(user);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente.");
            return "redirect:/admin/users";
        } catch (RuntimeException exception) {
            return userFormError(model, user, "No se pudo guardar el usuario: " + safeError(exception));
        }
    }

    /** Elimina usuario. */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        return "redirect:/admin/users";
    }

    /** Lista jugadores. */
    @GetMapping("/players")
    public String players(Model model) {
        model.addAttribute("items", playerRepository.findAllByOrderByDorsalAsc());
        return "admin/players";
    }

    /** Nuevo jugador. */
    @GetMapping("/players/new")
    public String newPlayer(Model model) {
        Player player = Player.builder().status("DISPONIBLE").position("Delantero").nationality("España").goals(0).assists(0).imageUrl("/images/avatar.svg").build();
        model.addAttribute("item", player);
        addPlayerFormOptions(model, player);
        return "admin/player-form";
    }

    /** Edita jugador. */
    @GetMapping("/players/{id}/edit")
    public String editPlayer(@PathVariable Long id, Model model) {
        Player player = playerRepository.findById(id).orElseThrow();
        model.addAttribute("item", player);
        addPlayerFormOptions(model, player);
        return "admin/player-form";
    }

    /** Guarda jugador y permite subir imagen. */
    @PostMapping("/players")
    public String savePlayer(@ModelAttribute("item") Player player, BindingResult bindingResult, @RequestParam(name = "playerImage", required = false) MultipartFile playerImage, Model model, RedirectAttributes redirectAttributes) {
        try {
            preservePlayerImage(player);
            if (bindingResult.hasErrors()) return playerFormError(model, player, bindingMessage("jugador", bindingResult));
            validatePlayer(player);
            if (hasFile(playerImage)) player.setImageUrl(storeUploadedImage(playerImage, "players", "jugador"));
            normalizePlayer(player);
            playerRepository.save(player);
            redirectAttributes.addFlashAttribute("success", "Jugador guardado correctamente.");
            return "redirect:/admin/players";
        } catch (RuntimeException | IOException exception) {
            preservePlayerImage(player);
            return playerFormError(model, player, "No se pudo guardar el jugador: " + safeError(exception));
        }
    }

    /** Elimina jugador. */
    @PostMapping("/players/{id}/delete")
    public String deletePlayer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        playerRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Jugador eliminado correctamente.");
        return "redirect:/admin/players";
    }

    /** Lista partidos. */
    @GetMapping("/matches")
    public String matches(Model model) {
        model.addAttribute("items", matchRepository.findAllByOrderByMatchDateAsc());
        return "admin/matches";
    }

    /** Nuevo partido. */
    @GetMapping("/matches/new")
    public String newMatch(Model model) {
        model.addAttribute("item", FootballMatch.builder()
            .competition("LaLiga Hypermotion")
            .matchDate(LocalDateTime.now().plusDays(7))
            .stadium("El Plantío")
            .basePrice(BigDecimal.valueOf(25))
            .availableTickets(5000)
            .status("PROGRAMADO")
            .homeGame(true)
            .imageUrl("/images/team-default-shield.svg")
            .build());
        addMatchFormOptions(model);
        return "admin/match-form";
    }

    /** Edita partido. */
    @GetMapping("/matches/{id}/edit")
    public String editMatch(@PathVariable Long id, Model model) {
        model.addAttribute("item", matchRepository.findById(id).orElseThrow());
        addMatchFormOptions(model);
        return "admin/match-form";
    }

    /** Guarda partido y permite subir el escudo del rival. */
    @PostMapping("/matches")
    public String saveMatch(@ModelAttribute("item") FootballMatch match, BindingResult bindingResult, @RequestParam(name = "opponentLogo", required = false) MultipartFile opponentLogo, Model model, RedirectAttributes redirectAttributes) {
        try {
            preserveMatchImage(match);
            if (bindingResult.hasErrors()) return matchFormError(model, match, bindingMessage("partido", bindingResult));
            validateMatch(match);
            if (hasFile(opponentLogo)) match.setImageUrl(storeUploadedImage(opponentLogo, "teams", "rival"));
            normalizeMatch(match);
            matchRepository.save(match);
            redirectAttributes.addFlashAttribute("success", "Partido guardado correctamente.");
            return "redirect:/admin/matches";
        } catch (RuntimeException | IOException exception) {
            preserveMatchImage(match);
            return matchFormError(model, match, "No se pudo guardar el partido: " + safeError(exception));
        }
    }

    /** Elimina partido. */
    @PostMapping("/matches/{id}/delete")
    public String deleteMatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Partido eliminado correctamente.");
        return "redirect:/admin/matches";
    }

    /** Lista productos. */
    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("items", productRepository.findAll());
        return "admin/products";
    }

    /** Nuevo producto. */
    @GetMapping("/products/new")
    public String newProduct(Model model) {
        Product product = Product.builder().active(true).stock(25).price(BigDecimal.TEN).category("Textil").imageUrl("/images/product.svg").build();
        model.addAttribute("item", product);
        addProductFormOptions(model);
        return "admin/product-form";
    }

    /** Edita producto. */
    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        model.addAttribute("item", productRepository.findById(id).orElseThrow());
        addProductFormOptions(model);
        return "admin/product-form";
    }

    /** Guarda producto y permite subir imagen. */
    @PostMapping("/products")
    public String saveProduct(@ModelAttribute("item") Product product, BindingResult bindingResult, @RequestParam(name = "productImage", required = false) MultipartFile productImage, Model model, RedirectAttributes redirectAttributes) {
        try {
            preserveProductImage(product);
            if (bindingResult.hasErrors()) return productFormError(model, product, bindingMessage("producto", bindingResult));
            validateProduct(product);
            if (hasFile(productImage)) product.setImageUrl(storeUploadedImage(productImage, "products", "producto"));
            normalizeProduct(product);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente.");
            return "redirect:/admin/products";
        } catch (RuntimeException | IOException exception) {
            preserveProductImage(product);
            return productFormError(model, product, "No se pudo guardar el producto: " + safeError(exception));
        }
    }

    /** Elimina producto. */
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente.");
        return "redirect:/admin/products";
    }

    /** Lista puntos del mapa. */
    @GetMapping("/map-points")
    public String mapPoints(Model model) {
        model.addAttribute("items", mapPointRepository.findAll());
        return "admin/map-points";
    }

    /** Nuevo punto del mapa. */
    @GetMapping("/map-points/new")
    public String newMapPoint(Model model) {
        model.addAttribute("item", MapPoint.builder().type("PARKING").latitude(BigDecimal.valueOf(42.3501)).longitude(BigDecimal.valueOf(-3.6892)).build());
        addMapPointFormOptions(model);
        return "admin/map-point-form";
    }

    /** Edita punto del mapa. */
    @GetMapping("/map-points/{id}/edit")
    public String editMapPoint(@PathVariable Long id, Model model) {
        model.addAttribute("item", mapPointRepository.findById(id).orElseThrow());
        addMapPointFormOptions(model);
        return "admin/map-point-form";
    }

    /** Guarda punto del mapa. */
    @PostMapping("/map-points")
    public String saveMapPoint(@ModelAttribute("item") MapPoint point, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) return mapPointFormError(model, point, bindingMessage("punto del mapa", bindingResult));
            validateMapPoint(point);
            mapPointRepository.save(point);
            redirectAttributes.addFlashAttribute("success", "Punto de mapa guardado correctamente.");
            return "redirect:/admin/map-points";
        } catch (RuntimeException exception) {
            return mapPointFormError(model, point, "No se pudo guardar el punto: " + safeError(exception));
        }
    }

    /** Elimina punto del mapa. */
    @PostMapping("/map-points/{id}/delete")
    public String deleteMapPoint(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        mapPointRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Punto de mapa eliminado correctamente.");
        return "redirect:/admin/map-points";
    }

    /** Lista sensores. */
    @GetMapping("/sensors")
    public String sensors(Model model) {
        model.addAttribute("items", sensorReadingRepository.findTop20ByOrderByCapturedAtDesc());
        return "admin/sensors";
    }

    /** Nuevo sensor. */
    @GetMapping("/sensors/new")
    public String newSensor(Model model) {
        model.addAttribute("item", SensorReading.builder().type("PARKING").status("NORMAL").value(BigDecimal.ZERO).unit("%").latitude(BigDecimal.valueOf(42.3501)).longitude(BigDecimal.valueOf(-3.6892)).capturedAt(LocalDateTime.now()).build());
        addSensorFormOptions(model);
        return "admin/sensor-form";
    }

    /** Edita sensor. */
    @GetMapping("/sensors/{id}/edit")
    public String editSensor(@PathVariable Long id, Model model) {
        model.addAttribute("item", sensorReadingRepository.findById(id).orElseThrow());
        addSensorFormOptions(model);
        return "admin/sensor-form";
    }

    /** Guarda sensor. */
    @PostMapping("/sensors")
    public String saveSensor(@ModelAttribute("item") SensorReading sensor, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) return sensorFormError(model, sensor, bindingMessage("sensor", bindingResult));
            validateSensor(sensor);
            if (sensor.getCapturedAt() == null) sensor.setCapturedAt(LocalDateTime.now());
            sensorReadingRepository.save(sensor);
            redirectAttributes.addFlashAttribute("success", "Sensor guardado correctamente.");
            return "redirect:/admin/sensors";
        } catch (RuntimeException exception) {
            return sensorFormError(model, sensor, "No se pudo guardar el sensor: " + safeError(exception));
        }
    }

    /** Elimina sensor. */
    @PostMapping("/sensors/{id}/delete")
    public String deleteSensor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        sensorReadingRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Sensor eliminado correctamente.");
        return "redirect:/admin/sensors";
    }

    /** Lista FAQs. */
    @GetMapping("/faqs")
    public String faqs(Model model) {
        model.addAttribute("items", faqRepository.findAll());
        return "admin/faqs";
    }

    /** Nueva FAQ. */
    @GetMapping("/faqs/new")
    public String newFaq(Model model) {
        model.addAttribute("item", Faq.builder().active(true).category("General").build());
        addFaqFormOptions(model);
        return "admin/faq-form";
    }

    /** Edita FAQ. */
    @GetMapping("/faqs/{id}/edit")
    public String editFaq(@PathVariable Long id, Model model) {
        model.addAttribute("item", faqRepository.findById(id).orElseThrow());
        addFaqFormOptions(model);
        return "admin/faq-form";
    }

    /** Guarda FAQ. */
    @PostMapping("/faqs")
    public String saveFaq(@ModelAttribute("item") Faq faq, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        try {
            if (bindingResult.hasErrors()) return faqFormError(model, faq, bindingMessage("FAQ", bindingResult));
            validateFaq(faq);
            faqRepository.save(faq);
            redirectAttributes.addFlashAttribute("success", "FAQ guardada correctamente.");
            return "redirect:/admin/faqs";
        } catch (RuntimeException exception) {
            return faqFormError(model, faq, "No se pudo guardar la FAQ: " + safeError(exception));
        }
    }

    /** Elimina FAQ. */
    @PostMapping("/faqs/{id}/delete")
    public String deleteFaq(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        faqRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "FAQ eliminada correctamente.");
        return "redirect:/admin/faqs";
    }

    /** Lista pedidos. */
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("items", orderRepository.findAll());
        return "admin/orders";
    }

    /** Lista logs del asistente. */
    @GetMapping("/assistant-logs")
    public String assistantLogs(Model model) {
        model.addAttribute("items", assistantLogRepository.findAll());
        return "admin/assistant-logs";
    }

    /** Exporta un listado administrativo a CSV. */
    @GetMapping("/{section}/export.csv")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportCsv(@PathVariable String section) {
        ExportData data = exportData(section);
        return adminExportService.csv(adminExportService.timestampedName(data.filename()), data.headers(), data.rows());
    }

    /** Exporta un listado administrativo a PDF. */
    @GetMapping("/{section}/export.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportPdf(@PathVariable String section) {
        ExportData data = exportData(section);
        return adminExportService.pdf(adminExportService.timestampedName(data.filename()), data.title(), data.headers(), data.rows());
    }

    /** Añade opciones auxiliares al formulario de jugador. */
    private void addPlayerFormOptions(Model model, Player current) {
        List<Integer> used = playerRepository.findAll().stream()
            .filter(player -> current.getId() == null || !player.getId().equals(current.getId()))
            .map(Player::getDorsal)
            .toList();
        List<Integer> available = new ArrayList<>();
        for (int dorsal = 1; dorsal <= 99; dorsal++) {
            if (!used.contains(dorsal) || (current.getDorsal() != null && current.getDorsal().equals(dorsal))) available.add(dorsal);
        }
        model.addAttribute("availableDorsals", available);
        model.addAttribute("positions", PLAYER_POSITIONS);
        model.addAttribute("playerStatuses", PLAYER_STATUSES);
    }

    /** Añade opciones auxiliares al formulario de partido. */
    private void addMatchFormOptions(Model model) {
        model.addAttribute("competitions", COMPETITIONS);
    }

    /** Añade opciones auxiliares al formulario de producto. */
    private void addProductFormOptions(Model model) {
        model.addAttribute("productCategories", PRODUCT_CATEGORIES);
    }

    /** Añade opciones auxiliares al formulario de punto de mapa. */
    private void addMapPointFormOptions(Model model) {
        model.addAttribute("mapPointTypes", MAP_POINT_TYPES);
    }

    /** Añade opciones auxiliares al formulario de sensor. */
    private void addSensorFormOptions(Model model) {
        model.addAttribute("sensorTypes", SENSOR_TYPES);
        model.addAttribute("sensorStatuses", SENSOR_STATUSES);
    }

    /** Añade opciones auxiliares al formulario de FAQ. */
    private void addFaqFormOptions(Model model) {
        model.addAttribute("faqCategories", FAQ_CATEGORIES);
    }

    /** Devuelve el formulario de usuario con error específico. */
    private String userFormError(Model model, AppUser user, String error) {
        model.addAttribute("item", user);
        model.addAttribute("error", error);
        return "admin/user-form";
    }

    /** Devuelve el formulario de jugador con error específico. */
    private String playerFormError(Model model, Player player, String error) {
        model.addAttribute("item", player);
        addPlayerFormOptions(model, player);
        model.addAttribute("error", error);
        return "admin/player-form";
    }

    /** Devuelve el formulario de partido con error específico. */
    private String matchFormError(Model model, FootballMatch match, String error) {
        model.addAttribute("item", match);
        addMatchFormOptions(model);
        model.addAttribute("error", error);
        return "admin/match-form";
    }

    /** Devuelve el formulario de producto con error específico. */
    private String productFormError(Model model, Product product, String error) {
        model.addAttribute("item", product);
        addProductFormOptions(model);
        model.addAttribute("error", error);
        return "admin/product-form";
    }

    /** Devuelve el formulario de punto de mapa con error específico. */
    private String mapPointFormError(Model model, MapPoint point, String error) {
        model.addAttribute("item", point);
        addMapPointFormOptions(model);
        model.addAttribute("error", error);
        return "admin/map-point-form";
    }

    /** Devuelve el formulario de sensor con error específico. */
    private String sensorFormError(Model model, SensorReading sensor, String error) {
        model.addAttribute("item", sensor);
        addSensorFormOptions(model);
        model.addAttribute("error", error);
        return "admin/sensor-form";
    }

    /** Devuelve el formulario de FAQ con error específico. */
    private String faqFormError(Model model, Faq faq, String error) {
        model.addAttribute("item", faq);
        addFaqFormOptions(model);
        model.addAttribute("error", error);
        return "admin/faq-form";
    }

    /** Valida campos obligatorios y reglas de jugador. */
    private void validatePlayer(Player player) {
        if (player.getName() == null || player.getName().isBlank()) throw new IllegalArgumentException("El nombre del jugador es obligatorio.");
        if (player.getDorsal() == null || player.getDorsal() < 1 || player.getDorsal() > 99) throw new IllegalArgumentException("Selecciona un dorsal disponible entre 1 y 99.");
        if (!PLAYER_POSITIONS.contains(player.getPosition())) throw new IllegalArgumentException("La posición seleccionada no es válida.");
        if (!PLAYER_STATUSES.contains(player.getStatus())) throw new IllegalArgumentException("El estado seleccionado no es válido.");
        if (player.getNationality() == null || player.getNationality().isBlank()) throw new IllegalArgumentException("La nacionalidad es obligatoria.");
        boolean duplicatedDorsal = playerRepository.findAll().stream()
            .anyMatch(existing -> existing.getDorsal() != null && existing.getDorsal().equals(player.getDorsal()) && (player.getId() == null || !existing.getId().equals(player.getId())));
        if (duplicatedDorsal) throw new IllegalArgumentException("El dorsal " + player.getDorsal() + " ya está asignado a otro jugador.");
    }

    /** Valida campos obligatorios y reglas de partido. */
    private void validateMatch(FootballMatch match) {
        if (match.getRival() == null || match.getRival().isBlank()) throw new IllegalArgumentException("El rival es obligatorio.");
        if (!COMPETITIONS.contains(match.getCompetition())) throw new IllegalArgumentException("La competición debe ser LaLiga Hypermotion o Copa del Rey.");
        if (match.getMatchDate() == null) throw new IllegalArgumentException("La fecha y hora del partido son obligatorias.");
        if (match.getBasePrice() == null || match.getBasePrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio base no puede ser negativo.");
        if (match.getAvailableTickets() == null || match.getAvailableTickets() < 0) throw new IllegalArgumentException("Las entradas disponibles no pueden ser negativas.");
    }

    /** Valida campos obligatorios y reglas de producto. */
    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().isBlank()) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        if (!PRODUCT_CATEGORIES.contains(product.getCategory())) throw new IllegalArgumentException("La categoría del producto no es válida.");
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");
        if (product.getStock() == null || product.getStock() < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
    }

    /** Valida campos obligatorios y reglas de punto de mapa. */
    private void validateMapPoint(MapPoint point) {
        if (point.getName() == null || point.getName().isBlank()) throw new IllegalArgumentException("El nombre del punto es obligatorio.");
        if (!MAP_POINT_TYPES.contains(point.getType())) throw new IllegalArgumentException("El tipo de punto seleccionado no es válido.");
        if (point.getLatitude() == null) throw new IllegalArgumentException("La latitud es obligatoria.");
        if (point.getLongitude() == null) throw new IllegalArgumentException("La longitud es obligatoria.");
    }

    /** Valida campos obligatorios y reglas de sensor. */
    private void validateSensor(SensorReading sensor) {
        if (sensor.getName() == null || sensor.getName().isBlank()) throw new IllegalArgumentException("El nombre del sensor es obligatorio.");
        if (!SENSOR_TYPES.contains(sensor.getType())) throw new IllegalArgumentException("El tipo de sensor seleccionado no es válido.");
        if (!SENSOR_STATUSES.contains(sensor.getStatus())) throw new IllegalArgumentException("El estado del sensor seleccionado no es válido.");
        if (sensor.getValue() == null) throw new IllegalArgumentException("El valor del sensor es obligatorio.");
        if (sensor.getUnit() == null || sensor.getUnit().isBlank()) throw new IllegalArgumentException("La unidad del sensor es obligatoria.");
        if (sensor.getLatitude() == null || sensor.getLongitude() == null) throw new IllegalArgumentException("La latitud y longitud son obligatorias.");
    }

    /** Valida campos obligatorios y reglas de FAQ. */
    private void validateFaq(Faq faq) {
        if (faq.getQuestion() == null || faq.getQuestion().isBlank()) throw new IllegalArgumentException("La pregunta de la FAQ es obligatoria.");
        if (faq.getAnswer() == null || faq.getAnswer().isBlank()) throw new IllegalArgumentException("La respuesta de la FAQ es obligatoria.");
        if (!FAQ_CATEGORIES.contains(faq.getCategory())) throw new IllegalArgumentException("La categoría seleccionada no es válida.");
    }

    /** Conserva imagen existente del jugador si se edita sin subir otra. */
    private void preservePlayerImage(Player player) {
        if (player.getId() != null && (player.getImageUrl() == null || player.getImageUrl().isBlank())) {
            playerRepository.findById(player.getId()).ifPresent(existing -> player.setImageUrl(existing.getImageUrl()));
        }
    }

    /** Conserva escudo existente del partido si se edita sin subir otro. */
    private void preserveMatchImage(FootballMatch match) {
        if (match.getId() != null && (match.getImageUrl() == null || match.getImageUrl().isBlank())) {
            matchRepository.findById(match.getId()).ifPresent(existing -> match.setImageUrl(existing.getImageUrl()));
        }
    }

    /** Conserva imagen existente del producto si se edita sin subir otra. */
    private void preserveProductImage(Product product) {
        if (product.getId() != null && (product.getImageUrl() == null || product.getImageUrl().isBlank())) {
            productRepository.findById(product.getId()).ifPresent(existing -> product.setImageUrl(existing.getImageUrl()));
        }
    }

    /** Normaliza datos de jugador antes de persistir. */
    private void normalizePlayer(Player player) {
        if (player.getStatus() == null || player.getStatus().isBlank()) player.setStatus("DISPONIBLE");
        if (player.getPosition() == null || player.getPosition().isBlank()) player.setPosition("Delantero");
        if (player.getNationality() == null || player.getNationality().isBlank()) player.setNationality("España");
        if (player.getGoals() == null) player.setGoals(0);
        if (player.getAssists() == null) player.setAssists(0);
        if (player.getImageUrl() == null || player.getImageUrl().isBlank()) player.setImageUrl("/images/avatar.svg");
    }

    /** Normaliza datos de partido antes de persistir. */
    private void normalizeMatch(FootballMatch match) {
        if (match.getImageUrl() == null || match.getImageUrl().isBlank()) match.setImageUrl("/images/team-default-shield.svg");
        if (match.getHomeGame() == null) match.setHomeGame(true);
        if (match.getStatus() == null || match.getStatus().isBlank()) match.setStatus("PROGRAMADO");
        if (match.getStadium() == null || match.getStadium().isBlank()) match.setStadium(Boolean.FALSE.equals(match.getHomeGame()) ? "Estadio rival" : "El Plantío");
        if (match.getAvailableTickets() == null) match.setAvailableTickets(0);
        if (match.getBasePrice() == null) match.setBasePrice(BigDecimal.ZERO);
    }

    /** Normaliza datos de producto antes de persistir. */
    private void normalizeProduct(Product product) {
        if (product.getPrice() == null) product.setPrice(BigDecimal.ZERO);
        if (product.getStock() == null) product.setStock(0);
        if (product.getCategory() == null || product.getCategory().isBlank()) product.setCategory("Textil");
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) product.setImageUrl("/images/product.svg");
    }

    /** Comprueba si el input contiene un fichero real. */
    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /** Guarda físicamente una imagen subida desde administración. */
    private String storeUploadedImage(MultipartFile file, String folder, String prefix) throws IOException {
        if (!hasFile(file)) return null;
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("La imagen supera el máximo permitido de 64MB. Selecciona una imagen más ligera.");
        String originalName = file.getOriginalFilename() == null ? prefix + ".png" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        if (!extension.matches("\\.(png|jpg|jpeg|webp|svg)")) throw new IllegalArgumentException("Formato de imagen no permitido. Usa PNG, JPG, JPEG, WEBP o SVG.");
        Path directory = Path.of("uploads", folder);
        Files.createDirectories(directory);
        String filename = prefix + "-" + UUID.randomUUID() + extension;
        try {
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IOException("No se pudo guardar la imagen en disco. Revisa el volumen de uploads o los permisos del contenedor.", exception);
        }
        return "/uploads/" + folder + "/" + filename;
    }

    /** Crea un mensaje legible para errores de binding. */
    private String bindingMessage(String entity, BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .findFirst()
            .map(error -> "Revisa el campo '" + error.getField() + "' del " + entity + ". Valor no válido: " + String.valueOf(error.getRejectedValue()))
            .orElse("Revisa los campos del formulario de " + entity + ".");
    }

    /** Limpia mensajes de error para mostrarlos al usuario. */
    private String safeError(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) return "revisa los campos del formulario.";
        return exception.getMessage();
    }

    /** Construye los datos exportables de cada sección. */
    private ExportData exportData(String section) {
        return switch (section) {
            case "users" -> new ExportData("usuarios", "Usuarios", List.of("ID", "Nombre", "Email", "Rol", "Activo"),
                userService.findAll().stream().map(user -> List.of(value(user.getId()), user.getFullName(), value(user.getEmail()), value(user.getRole()), value(user.isEnabled()))).toList());
            case "players" -> new ExportData("jugadores", "Jugadores", List.of("Dorsal", "Nombre", "Posición", "Nacionalidad", "Estado", "Goles", "Asistencias"),
                playerRepository.findAllByOrderByDorsalAsc().stream().map(player -> List.of(value(player.getDorsal()), value(player.getName()), value(player.getPosition()), value(player.getNationality()), value(player.getStatus()), value(player.getGoals()), value(player.getAssists()))).toList());
            case "matches" -> new ExportData("partidos", "Partidos", List.of("Rival", "Competición", "Fecha", "Estadio", "Localía", "Entradas", "Precio", "Estado"),
                matchRepository.findAllByOrderByMatchDateAsc().stream().map(match -> List.of(value(match.getRival()), value(match.getCompetition()), date(match.getMatchDate()), value(match.getStadium()), Boolean.FALSE.equals(match.getHomeGame()) ? "Visitante" : "Local", value(match.getAvailableTickets()), value(match.getBasePrice()), value(match.getStatus()))).toList());
            case "products" -> new ExportData("productos", "Productos", List.of("Nombre", "Categoría", "Precio", "Stock", "Activo"),
                productRepository.findAll().stream().map(product -> List.of(value(product.getName()), value(product.getCategory()), value(product.getPrice()), value(product.getStock()), value(product.isActive()))).toList());
            case "map-points" -> new ExportData("puntos-mapa", "Puntos del mapa", List.of("Nombre", "Tipo", "Latitud", "Longitud", "Descripción"),
                mapPointRepository.findAll().stream().map(point -> List.of(value(point.getName()), value(point.getType()), value(point.getLatitude()), value(point.getLongitude()), value(point.getDescription()))).toList());
            case "sensors" -> new ExportData("sensores", "Sensores", List.of("Nombre", "Tipo", "Valor", "Unidad", "Estado", "Fecha"),
                sensorReadingRepository.findTop20ByOrderByCapturedAtDesc().stream().map(sensor -> List.of(value(sensor.getName()), value(sensor.getType()), value(sensor.getValue()), value(sensor.getUnit()), value(sensor.getStatus()), date(sensor.getCapturedAt()))).toList());
            case "faqs" -> new ExportData("faqs", "FAQs", List.of("Pregunta", "Categoría", "Activa", "Respuesta"),
                faqRepository.findAll().stream().map(faq -> List.of(value(faq.getQuestion()), value(faq.getCategory()), value(faq.isActive()), value(faq.getAnswer()))).toList());
            case "orders" -> new ExportData("pedidos", "Pedidos", List.of("Referencia", "Usuario", "Fecha", "Estado", "Total"),
                orderRepository.findAll().stream().map(order -> List.of(value(order.getPaymentReference()), order.getUser() == null ? "" : value(order.getUser().getEmail()), date(order.getCreatedAt()), value(order.getStatus()), value(order.getTotal()))).toList());
            case "assistant-logs" -> new ExportData("logs-ia", "Logs IA", List.of("Usuario", "Pregunta", "Fuente", "Fecha"),
                assistantLogRepository.findAll().stream().map(log -> List.of(log.getUser() == null ? "" : value(log.getUser().getEmail()), value(log.getQuestion()), value(log.getSource()), date(log.getCreatedAt()))).toList());
            default -> throw new IllegalArgumentException("Sección no exportable: " + section);
        };
    }

    /** Convierte valores nulos a texto vacío. */
    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Formatea fechas nulas o no nulas. */
    private String date(LocalDateTime date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    /** Datos de exportación de una sección administrativa. */
    private record ExportData(String filename, String title, List<String> headers, List<List<String>> rows) { }
}
