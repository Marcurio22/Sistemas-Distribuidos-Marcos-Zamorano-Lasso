/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.AdminMatchForm;
import com.marcos.plantio360.dto.AdminPlayerForm;
import com.marcos.plantio360.dto.AdminUserForm;
import com.marcos.plantio360.model.*;
import com.marcos.plantio360.repository.*;
import com.marcos.plantio360.service.AdminExportService;
import com.marcos.plantio360.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Controlador de administración para CRUD de entidades principales y exportaciones.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HTML_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final long MAX_IMAGE_BYTES = 64L * 1024L * 1024L;
    private static final String DEFAULT_AVATAR = "/images/avatar.svg";
    private static final String DEFAULT_TEAM_SHIELD = "/images/team-default-shield.svg";
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
        model.addAttribute("item", AdminUserForm.empty());
        return "admin/user-form";
    }

    /** Edición de usuario. */
    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        model.addAttribute("item", AdminUserForm.from(userService.findById(id)));
        return "admin/user-form";
    }

    /** Guarda usuario sin permitir que el administrador cambie contraseñas. */
    @PostMapping("/users")
    public String saveUser(@ModelAttribute("item") AdminUserForm form,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Long id = parseId(form.getId(), "usuario");
            AppUser user = id == null ? AppUser.builder().avatarUrl(DEFAULT_AVATAR).build() : userService.findById(id);
            String email = normalizeEmail(form.getEmail());
            if (isBlank(form.getFirstName())) throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
            if (isBlank(form.getLastName())) throw new IllegalArgumentException("Los apellidos del usuario son obligatorios.");
            if (isBlank(email) || !email.contains("@")) throw new IllegalArgumentException("Introduce un correo electrónico válido.");
            if (isEmailInUse(email, id)) throw new IllegalArgumentException("Ya existe otro usuario con ese correo electrónico.");
            if (!List.of("ROLE_USER", "ROLE_ADMIN").contains(form.getRole())) throw new IllegalArgumentException("El rol seleccionado no es válido.");
            user.setFirstName(form.getFirstName().trim());
            user.setLastName(form.getLastName().trim());
            user.setEmail(email);
            user.setPhone(emptyToNull(form.getPhone()));
            user.setRole(form.getRole());
            user.setEnabled(Boolean.TRUE.equals(form.getEnabled()));
            user.setAvatarUrl(isBlank(form.getAvatarUrl()) ? DEFAULT_AVATAR : form.getAvatarUrl());
            userService.saveAdmin(user);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado correctamente.");
            return "redirect:/admin/users";
        } catch (RuntimeException exception) {
            return userFormError(model, form, "No se pudo guardar el usuario: " + friendly(exception));
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
        return playerForm(model, AdminPlayerForm.empty(), null);
    }

    /** Edita jugador. */
    @GetMapping("/players/{id}/edit")
    public String editPlayer(@PathVariable Long id, Model model) {
        return playerForm(model, AdminPlayerForm.from(findPlayer(id)), null);
    }

    /** Guarda jugador y permite subir imagen. */
    @PostMapping(value = "/players", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String savePlayer(HttpServletRequest request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        log.info("POST /admin/players recibido. contentLength={}, contentType={}", request.getContentLengthLong(), request.getContentType());
        AdminPlayerForm form = AdminPlayerForm.empty();
        try {
            form = readPlayerForm(request);
            MultipartFile playerImage = multipartFile(request, "playerImage");
            Long id = parseId(form.getId(), "jugador");
            Player player = id == null ? new Player() : findPlayer(id);
            String previousImage = player.getImageUrl();
            Integer dorsalNumber = parseInteger(form.getDorsal(), "dorsal", true);
            if (dorsalNumber == null) throw new IllegalArgumentException("Selecciona un dorsal disponible.");
            if (dorsalNumber < 1 || dorsalNumber > 99) throw new IllegalArgumentException("El dorsal debe estar entre 1 y 99.");
            if (isDorsalInUse(dorsalNumber, id)) throw new IllegalArgumentException("El dorsal " + dorsalNumber + " ya está asignado a otro jugador.");
            if (isBlank(form.getName())) throw new IllegalArgumentException("El nombre del jugador es obligatorio.");
            if (!PLAYER_POSITIONS.contains(form.getPosition())) throw new IllegalArgumentException("Selecciona una posición válida.");
            if (isBlank(form.getNationality())) throw new IllegalArgumentException("La nacionalidad del jugador es obligatoria.");
            if (!PLAYER_STATUSES.contains(form.getStatus())) throw new IllegalArgumentException("Selecciona un estado válido.");
            Integer parsedAge = parseInteger(form.getAge(), "edad", false);
            BigDecimal parsedHeight = parseBigDecimal(form.getHeight(), "altura", false);
            BigDecimal parsedWeight = parseBigDecimal(form.getWeight(), "peso", false);
            Integer parsedGoals = parseInteger(form.getGoals(), "goles", false);
            Integer parsedAssists = parseInteger(form.getAssists(), "asistencias", false);
            if (parsedAge != null && (parsedAge < 16 || parsedAge > 60)) throw new IllegalArgumentException("La edad debe estar entre 16 y 60 años.");
            if (parsedHeight != null && (parsedHeight.compareTo(BigDecimal.valueOf(1.40)) < 0 || parsedHeight.compareTo(BigDecimal.valueOf(2.20)) > 0)) throw new IllegalArgumentException("La altura debe estar entre 1.40 y 2.20 metros.");
            if (parsedWeight != null && (parsedWeight.compareTo(BigDecimal.valueOf(45)) < 0 || parsedWeight.compareTo(BigDecimal.valueOf(120)) > 0)) throw new IllegalArgumentException("El peso debe estar entre 45 y 120 kg.");
            if (parsedGoals != null && parsedGoals < 0) throw new IllegalArgumentException("Los goles no pueden ser negativos.");
            if (parsedAssists != null && parsedAssists < 0) throw new IllegalArgumentException("Las asistencias no pueden ser negativas.");
            player.setDorsal(dorsalNumber);
            player.setName(form.getName().trim());
            player.setPosition(form.getPosition());
            player.setNationality(form.getNationality().trim());
            player.setStatus(form.getStatus());
            player.setAge(parsedAge);
            player.setHeight(parsedHeight);
            player.setWeight(parsedWeight);
            player.setGoals(parsedGoals == null ? 0 : parsedGoals);
            player.setAssists(parsedAssists == null ? 0 : parsedAssists);
            player.setDescription(emptyToNull(form.getDescription()));
            if (playerImage != null && !playerImage.isEmpty()) {
                player.setImageUrl(storeUploadedImage(playerImage, "players", "jugador"));
            } else if (!isBlank(previousImage)) {
                player.setImageUrl(previousImage);
            } else {
                player.setImageUrl(DEFAULT_AVATAR);
            }
            playerRepository.save(player);
            redirectAttributes.addFlashAttribute("success", "Jugador guardado correctamente.");
            return "redirect:/admin/players";
        } catch (MaxUploadSizeExceededException exception) {
            return playerForm(model, form, "La imagen supera el tamaño máximo permitido de 64MB. Selecciona una imagen más ligera.");
        } catch (MultipartException exception) {
            return playerForm(model, form, "No se pudo leer el formulario multipart. Si adjuntaste una imagen, comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.");
        } catch (IllegalStateException exception) {
            if (isMultipartFailure(exception)) {
                return playerForm(model, form, "No se pudo leer el formulario multipart. Si adjuntaste una imagen, comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.");
            }
            return playerForm(model, form, "No se pudo guardar el jugador: " + friendly(exception));
        } catch (RuntimeException | IOException exception) {
            return playerForm(model, form, "No se pudo guardar el jugador: " + friendly(exception));
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
        FootballMatch match = FootballMatch.builder()
            .competition("LaLiga Hypermotion")
            .matchDate(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0))
            .stadium("El Plantío")
            .basePrice(BigDecimal.valueOf(25))
            .availableTickets(5000)
            .status("PROGRAMADO")
            .homeGame(true)
            .imageUrl(DEFAULT_TEAM_SHIELD)
            .build();
        return matchForm(model, AdminMatchForm.from(match, HTML_DATETIME), null);
    }

    /** Edita partido. */
    @GetMapping("/matches/{id}/edit")
    public String editMatch(@PathVariable Long id, Model model) {
        return matchForm(model, AdminMatchForm.from(findMatch(id), HTML_DATETIME), null);
    }

    /** Guarda partido y permite subir el escudo del rival. */
    @PostMapping(value = "/matches", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String saveMatch(HttpServletRequest request,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        log.info("POST /admin/matches recibido. contentLength={}, contentType={}", request.getContentLengthLong(), request.getContentType());
        AdminMatchForm form = AdminMatchForm.builder().homeGame(true).build();
        try {
            form = readMatchForm(request);
            MultipartFile opponentLogo = multipartFile(request, "opponentLogo");
            Long id = parseId(form.getId(), "partido");
            FootballMatch match = id == null ? new FootballMatch() : findMatch(id);
            String previousImage = match.getImageUrl();
            if (isBlank(form.getRival())) throw new IllegalArgumentException("El rival es obligatorio.");
            if (!COMPETITIONS.contains(form.getCompetition())) throw new IllegalArgumentException("Selecciona una competición válida.");
            LocalDateTime parsedDate = parseHtmlDateTime(form.getMatchDateValue());
            BigDecimal parsedPrice = parseBigDecimal(form.getBasePrice(), "precio base", true);
            Integer parsedTickets = parseInteger(form.getAvailableTickets(), "entradas disponibles", true);
            if (parsedPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio base no puede ser negativo.");
            if (parsedTickets < 0) throw new IllegalArgumentException("Las entradas disponibles no pueden ser negativas.");
            if (!List.of("PROGRAMADO", "JUGADO", "CANCELADO").contains(form.getStatus())) throw new IllegalArgumentException("Selecciona un estado válido.");
            boolean homeGame = !Boolean.FALSE.equals(form.getHomeGame());
            match.setRival(form.getRival().trim());
            match.setCompetition(form.getCompetition());
            match.setMatchDate(parsedDate);
            match.setStadium(isBlank(form.getStadium()) ? (homeGame ? "El Plantío" : "Estadio rival") : form.getStadium().trim());
            match.setBasePrice(parsedPrice);
            match.setAvailableTickets(parsedTickets);
            match.setStatus(form.getStatus());
            match.setHomeGame(homeGame);
            if (opponentLogo != null && !opponentLogo.isEmpty()) {
                match.setImageUrl(storeUploadedImage(opponentLogo, "teams", "rival"));
            } else if (!isBlank(previousImage)) {
                match.setImageUrl(previousImage);
            } else {
                match.setImageUrl(DEFAULT_TEAM_SHIELD);
            }
            matchRepository.save(match);
            redirectAttributes.addFlashAttribute("success", "Partido guardado correctamente.");
            return "redirect:/admin/matches";
        } catch (MaxUploadSizeExceededException exception) {
            return matchForm(model, form, "La imagen supera el tamaño máximo permitido de 64MB. Selecciona una imagen más ligera.");
        } catch (MultipartException exception) {
            return matchForm(model, form, "No se pudo leer el formulario multipart. Si adjuntaste una imagen, comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.");
        } catch (IllegalStateException exception) {
            if (isMultipartFailure(exception)) {
                return matchForm(model, form, "No se pudo leer el formulario multipart. Si adjuntaste una imagen, comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.");
            }
            return matchForm(model, form, "No se pudo guardar el partido: " + friendly(exception));
        } catch (RuntimeException | IOException exception) {
            return matchForm(model, form, "No se pudo guardar el partido: " + friendly(exception));
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
        return productForm(model, product, null);
    }

    /** Edita producto. */
    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        return productForm(model, productRepository.findById(id).orElseThrow(), null);
    }

    /** Guarda producto y permite subir imagen. */
    @PostMapping("/products")
    public String saveProduct(@RequestParam(required = false) Long id,
                              @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam String price,
                              @RequestParam String stock,
                              @RequestParam String category,
                              @RequestParam(name = "active", defaultValue = "false") boolean active,
                              @RequestParam(name = "productImage", required = false) MultipartFile productImage,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        Product product = id == null ? new Product() : productRepository.findById(id).orElseThrow();
        String previousImage = product.getImageUrl();
        try {
            if (isBlank(name)) throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            if (!PRODUCT_CATEGORIES.contains(category)) throw new IllegalArgumentException("Selecciona una categoría válida.");
            BigDecimal parsedPrice = parseBigDecimal(price, "precio", true);
            Integer parsedStock = parseInteger(stock, "stock", true);
            if (parsedPrice.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");
            if (parsedStock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
            product.setName(name.trim());
            product.setDescription(emptyToNull(description));
            product.setPrice(parsedPrice);
            product.setStock(parsedStock);
            product.setCategory(category);
            product.setActive(active);
            if (productImage != null && !productImage.isEmpty()) {
                product.setImageUrl(storeUploadedImage(productImage, "products", "producto"));
            } else if (!isBlank(previousImage)) {
                product.setImageUrl(previousImage);
            } else {
                product.setImageUrl("/images/product.svg");
            }
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente.");
            return "redirect:/admin/products";
        } catch (RuntimeException | IOException exception) {
            if (isBlank(product.getImageUrl())) product.setImageUrl(previousImage == null ? "/images/product.svg" : previousImage);
            return productForm(model, product, "No se pudo guardar el producto: " + friendly(exception));
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
        MapPoint point = MapPoint.builder().type("PARKING").latitude(BigDecimal.valueOf(42.3501)).longitude(BigDecimal.valueOf(-3.6892)).build();
        return mapPointForm(model, point, null);
    }

    /** Edita punto del mapa. */
    @GetMapping("/map-points/{id}/edit")
    public String editMapPoint(@PathVariable Long id, Model model) {
        return mapPointForm(model, mapPointRepository.findById(id).orElseThrow(), null);
    }

    /** Guarda punto del mapa. */
    @PostMapping("/map-points")
    public String saveMapPoint(@RequestParam(required = false) Long id,
                               @RequestParam String name,
                               @RequestParam String type,
                               @RequestParam String latitude,
                               @RequestParam String longitude,
                               @RequestParam(required = false) String description,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        MapPoint point = id == null ? new MapPoint() : mapPointRepository.findById(id).orElseThrow();
        try {
            if (isBlank(name)) throw new IllegalArgumentException("El nombre del punto es obligatorio.");
            if (!MAP_POINT_TYPES.contains(type)) throw new IllegalArgumentException("Selecciona un tipo de punto válido.");
            point.setName(name.trim());
            point.setType(type);
            point.setLatitude(parseBigDecimal(latitude, "latitud", true));
            point.setLongitude(parseBigDecimal(longitude, "longitud", true));
            point.setDescription(emptyToNull(description));
            mapPointRepository.save(point);
            redirectAttributes.addFlashAttribute("success", "Punto de mapa guardado correctamente.");
            return "redirect:/admin/map-points";
        } catch (RuntimeException exception) {
            return mapPointForm(model, point, "No se pudo guardar el punto de mapa: " + friendly(exception));
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
        SensorReading sensor = SensorReading.builder().type("PARKING").status("NORMAL").value(BigDecimal.ZERO).unit("%").latitude(BigDecimal.valueOf(42.3501)).longitude(BigDecimal.valueOf(-3.6892)).capturedAt(LocalDateTime.now()).build();
        return sensorForm(model, sensor, null);
    }

    /** Edita sensor. */
    @GetMapping("/sensors/{id}/edit")
    public String editSensor(@PathVariable Long id, Model model) {
        return sensorForm(model, sensorReadingRepository.findById(id).orElseThrow(), null);
    }

    /** Guarda sensor. */
    @PostMapping("/sensors")
    public String saveSensor(@RequestParam(required = false) Long id,
                             @RequestParam String name,
                             @RequestParam String type,
                             @RequestParam String value,
                             @RequestParam String unit,
                             @RequestParam String status,
                             @RequestParam String latitude,
                             @RequestParam String longitude,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        SensorReading sensor = id == null ? new SensorReading() : sensorReadingRepository.findById(id).orElseThrow();
        try {
            if (isBlank(name)) throw new IllegalArgumentException("El nombre del sensor es obligatorio.");
            if (!SENSOR_TYPES.contains(type)) throw new IllegalArgumentException("Selecciona un tipo de sensor válido.");
            if (isBlank(unit)) throw new IllegalArgumentException("La unidad del sensor es obligatoria.");
            if (!SENSOR_STATUSES.contains(status)) throw new IllegalArgumentException("Selecciona un estado de sensor válido.");
            sensor.setName(name.trim());
            sensor.setType(type);
            sensor.setValue(parseBigDecimal(value, "valor", true));
            sensor.setUnit(unit.trim());
            sensor.setStatus(status);
            sensor.setLatitude(parseBigDecimal(latitude, "latitud", true));
            sensor.setLongitude(parseBigDecimal(longitude, "longitud", true));
            if (sensor.getCapturedAt() == null) sensor.setCapturedAt(LocalDateTime.now());
            sensorReadingRepository.save(sensor);
            redirectAttributes.addFlashAttribute("success", "Sensor guardado correctamente.");
            return "redirect:/admin/sensors";
        } catch (RuntimeException exception) {
            return sensorForm(model, sensor, "No se pudo guardar el sensor: " + friendly(exception));
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
        return faqForm(model, Faq.builder().active(true).category("General").build(), null);
    }

    /** Edita FAQ. */
    @GetMapping("/faqs/{id}/edit")
    public String editFaq(@PathVariable Long id, Model model) {
        return faqForm(model, faqRepository.findById(id).orElseThrow(), null);
    }

    /** Guarda FAQ. */
    @PostMapping("/faqs")
    public String saveFaq(@RequestParam(required = false) Long id,
                          @RequestParam String question,
                          @RequestParam String answer,
                          @RequestParam String category,
                          @RequestParam(name = "active", defaultValue = "false") boolean active,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        Faq faq = id == null ? new Faq() : faqRepository.findById(id).orElseThrow();
        try {
            if (isBlank(question)) throw new IllegalArgumentException("La pregunta de la FAQ es obligatoria.");
            if (isBlank(answer)) throw new IllegalArgumentException("La respuesta de la FAQ es obligatoria.");
            if (!FAQ_CATEGORIES.contains(category)) throw new IllegalArgumentException("Selecciona una categoría de FAQ válida.");
            faq.setQuestion(question.trim());
            faq.setAnswer(answer.trim());
            faq.setCategory(category);
            faq.setActive(active);
            faqRepository.save(faq);
            redirectAttributes.addFlashAttribute("success", "FAQ guardada correctamente.");
            return "redirect:/admin/faqs";
        } catch (RuntimeException exception) {
            return faqForm(model, faq, "No se pudo guardar la FAQ: " + friendly(exception));
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

    /** Construye y devuelve formulario de usuario con error opcional. */
    private String userFormError(Model model, AdminUserForm user, String error) {
        model.addAttribute("item", user);
        model.addAttribute("error", error);
        return "admin/user-form";
    }

    /** Construye y devuelve formulario de jugador con error opcional. */
    private String playerForm(Model model, AdminPlayerForm player, String error) {
        model.addAttribute("item", player);
        addPlayerFormOptions(model, player);
        if (error != null) model.addAttribute("error", error);
        return "admin/player-form";
    }

    /** Construye y devuelve formulario de partido con error opcional. */
    private String matchForm(Model model, AdminMatchForm match, String error) {
        model.addAttribute("item", match);
        addMatchFormOptions(model);
        if (error != null) model.addAttribute("error", error);
        return "admin/match-form";
    }

    /** Construye y devuelve formulario de producto con error opcional. */
    private String productForm(Model model, Product product, String error) {
        model.addAttribute("item", product);
        addProductFormOptions(model);
        if (error != null) model.addAttribute("error", error);
        return "admin/product-form";
    }

    /** Construye y devuelve formulario de punto de mapa con error opcional. */
    private String mapPointForm(Model model, MapPoint point, String error) {
        model.addAttribute("item", point);
        addMapPointFormOptions(model);
        if (error != null) model.addAttribute("error", error);
        return "admin/map-point-form";
    }

    /** Construye y devuelve formulario de sensor con error opcional. */
    private String sensorForm(Model model, SensorReading sensor, String error) {
        model.addAttribute("item", sensor);
        addSensorFormOptions(model);
        if (error != null) model.addAttribute("error", error);
        return "admin/sensor-form";
    }

    /** Construye y devuelve formulario de FAQ con error opcional. */
    private String faqForm(Model model, Faq faq, String error) {
        model.addAttribute("item", faq);
        model.addAttribute("faqCategories", FAQ_CATEGORIES);
        if (error != null) model.addAttribute("error", error);
        return "admin/faq-form";
    }

    /** Añade opciones auxiliares al formulario de jugador. */
    private void addPlayerFormOptions(Model model, AdminPlayerForm current) {
        Long currentId = parseIdOrNull(current.getId());
        Integer currentDorsal = parseIntegerOrNull(current.getDorsal());
        List<Integer> used = playerRepository.findAll().stream()
            .filter(player -> currentId == null || !player.getId().equals(currentId))
            .map(Player::getDorsal)
            .toList();
        List<Integer> available = new ArrayList<>();
        for (int dorsal = 1; dorsal <= 99; dorsal++) {
            if (!used.contains(dorsal) || (currentDorsal != null && currentDorsal.equals(dorsal))) {
                available.add(dorsal);
            }
        }
        if (currentDorsal != null && currentDorsal >= 1 && currentDorsal <= 99 && !available.contains(currentDorsal)) {
            available.add(currentDorsal);
            available.sort(Integer::compareTo);
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

    /** Comprueba si un dorsal ya está ocupado por otro jugador. */
    private boolean isDorsalInUse(Integer dorsal, Long currentId) {
        return currentId == null ? playerRepository.existsByDorsal(dorsal) : playerRepository.existsByDorsalAndIdNot(dorsal, currentId);
    }

    /** Lee el formulario multipart de jugador dentro del try/catch del controlador. */
    private AdminPlayerForm readPlayerForm(HttpServletRequest request) {
        return AdminPlayerForm.builder()
            .id(request.getParameter("id"))
            .dorsal(request.getParameter("dorsal"))
            .name(request.getParameter("name"))
            .position(request.getParameter("position"))
            .nationality(request.getParameter("nationality"))
            .status(request.getParameter("status"))
            .age(request.getParameter("age"))
            .height(request.getParameter("height"))
            .weight(request.getParameter("weight"))
            .goals(request.getParameter("goals"))
            .assists(request.getParameter("assists"))
            .description(request.getParameter("description"))
            .imageUrl(request.getParameter("imageUrl"))
            .build();
    }

    /** Lee el formulario multipart de partido dentro del try/catch del controlador. */
    private AdminMatchForm readMatchForm(HttpServletRequest request) {
        return AdminMatchForm.builder()
            .id(request.getParameter("id"))
            .rival(request.getParameter("rival"))
            .competition(request.getParameter("competition"))
            .matchDateValue(request.getParameter("matchDateValue"))
            .stadium(request.getParameter("stadium"))
            .basePrice(request.getParameter("basePrice"))
            .availableTickets(request.getParameter("availableTickets"))
            .status(request.getParameter("status"))
            .homeGame(!"false".equalsIgnoreCase(request.getParameter("homeGame")))
            .imageUrl(request.getParameter("imageUrl"))
            .build();
    }

    /** Obtiene un archivo de una petición multipart ya envuelta por Spring MVC. */
    private MultipartFile multipartFile(HttpServletRequest request, String fieldName) {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            return multipartRequest.getFile(fieldName);
        }
        return null;
    }

    /** Parsea el identificador oculto del formulario. */
    private Long parseId(String value, String entityName) {
        if (isBlank(value)) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El identificador de " + entityName + " no es válido.");
        }
    }

    /** Parsea un identificador sin lanzar excepción para reconstruir listas de opciones. */
    private Long parseIdOrNull(String value) {
        try {
            return parseId(value, "registro");
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /** Parsea un entero sin lanzar excepción para reconstruir listas de opciones. */
    private Integer parseIntegerOrNull(String value) {
        try {
            return parseInteger(value, "valor", false);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /** Localiza jugador con mensaje apto para formulario. */
    private Player findPlayer(Long id) {
        return playerRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe el jugador indicado."));
    }

    /** Localiza partido con mensaje apto para formulario. */
    private FootballMatch findMatch(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe el partido indicado."));
    }

    /** Comprueba si el correo ya pertenece a otro usuario. */
    private boolean isEmailInUse(String email, Long currentId) {
        return currentId == null ? userService.existsByEmail(email) : userService.existsByEmailAndIdNot(email, currentId);
    }

    /** Normaliza correo electrónico para comparar y persistir. */
    private String normalizeEmail(String email) {
        return isBlank(email) ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Parsea un entero enviado como texto desde formularios administrativos. */
    private Integer parseInteger(String value, String fieldName, boolean required) {
        if (isBlank(value)) {
            if (required) throw new IllegalArgumentException("El campo " + fieldName + " es obligatorio.");
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe ser un número entero válido.");
        }
    }

    /** Parsea un decimal enviado como texto desde formularios administrativos. */
    private BigDecimal parseBigDecimal(String value, String fieldName, boolean required) {
        if (isBlank(value)) {
            if (required) throw new IllegalArgumentException("El campo " + fieldName + " es obligatorio.");
            return null;
        }
        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El campo " + fieldName + " debe ser un número válido.");
        }
    }

    /** Guarda físicamente una imagen subida desde administración. */
    private String storeUploadedImage(MultipartFile file, String folder, String prefix) throws IOException {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("La imagen supera el máximo permitido de 64MB.");
        String originalName = file.getOriginalFilename() == null ? prefix + ".png" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        if (!extension.matches("\\.(png|jpg|jpeg|webp|svg)")) throw new IllegalArgumentException("Formato de imagen no permitido. Usa PNG, JPG, JPEG, WEBP o SVG.");
        Path directory = Path.of("uploads", folder);
        Files.createDirectories(directory);
        String filename = prefix + "-" + UUID.randomUUID() + extension;
        Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + folder + "/" + filename;
    }

    /** Parsea fecha HTML datetime-local con error específico. */
    private LocalDateTime parseHtmlDateTime(String value) {
        if (isBlank(value)) throw new IllegalArgumentException("La fecha y hora del partido son obligatorias.");
        try {
            return LocalDateTime.parse(value.trim(), HTML_DATETIME);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("La fecha y hora del partido no tienen un formato válido.");
        }
    }

    /** Distingue excepciones de multipart de otros IllegalStateException. */
    private boolean isMultipartFailure(Exception exception) {
        String message = exception.getMessage();
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("multipart")
            || lower.contains("multi-part")
            || lower.contains("parts")
            || lower.contains("size")
            || lower.contains("request body")
            || lower.contains("too large");
    }

    /** Limpia mensajes técnicos para mostrarlos al usuario. */
    private String friendly(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) return "revisa los campos del formulario.";
        String message = exception.getMessage();
        if (message.contains("Duplicate entry") || message.contains("constraint") || message.contains("uk_players_dorsal")) return "ya existe un registro con esos datos únicos.";
        if (message.contains("Failed to convert") || message.contains("typeMismatch")) return "revisa los valores numéricos y de fecha.";
        return message;
    }

    /** Comprueba cadena vacía. */
    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    /** Convierte vacío a null. */
    private String emptyToNull(String value) { return isBlank(value) ? null : value.trim(); }

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
    private String value(Object value) { return value == null ? "" : String.valueOf(value); }

    /** Formatea fechas nulas o no nulas. */
    private String date(LocalDateTime date) { return date == null ? "" : date.format(DATE_FORMAT); }

    /** Datos de exportación de una sección administrativa. */
    private record ExportData(String filename, String title, List<String> headers, List<List<String>> rows) { }
}
