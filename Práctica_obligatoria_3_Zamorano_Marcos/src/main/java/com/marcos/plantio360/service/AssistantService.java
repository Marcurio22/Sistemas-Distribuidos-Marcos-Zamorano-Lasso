/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.dto.AssistantResponse;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.model.AssistantLog;
import com.marcos.plantio360.model.Faq;
import com.marcos.plantio360.model.FootballMatch;
import com.marcos.plantio360.model.Player;
import com.marcos.plantio360.model.Product;
import com.marcos.plantio360.repository.AssistantLogRepository;
import com.marcos.plantio360.repository.FaqRepository;
import com.marcos.plantio360.repository.FootballMatchRepository;
import com.marcos.plantio360.repository.PlayerRepository;
import com.marcos.plantio360.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio del asistente Blanquinegro Bot con FAQ local, datos vivos de la app y fallback Gemini.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SYSTEM_CONTEXT = """
        Eres Blanquinegro Bot, asistente de El Plantío 360, una plataforma académica para aficionados al fútbol.
        Responde siempre en español, con tono cercano, breve y útil.
        No inventes datos de partidos, productos, stock, precios, jugadores ni entradas.
        Cuando la pregunta dependa de datos internos, usa exclusivamente el contexto de aplicación recibido en el mensaje del usuario.
        Si el contexto no contiene un dato, responde que no consta en la aplicación.
        Aclara cuando proceda que se trata de un prototipo académico con datos simulados.
        """;

    private final FaqRepository faqRepository;
    private final ProductRepository productRepository;
    private final FootballMatchRepository footballMatchRepository;
    private final PlayerRepository playerRepository;
    private final AssistantLogRepository assistantLogRepository;
    private final RestTemplate restTemplate;

    @Value("${plantio.gemini.api-key:${spring.ai.google.genai.api-key:}}")
    private String geminiApiKey;

    @Value("${plantio.gemini.enabled:true}")
    private boolean geminiEnabled;

    @Value("${plantio.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    /**
     * Responde una pregunta buscando primero en la FAQ local, después en datos vivos y finalmente en Gemini.
     *
     * @param user usuario que pregunta.
     * @param question pregunta textual.
     * @return respuesta estructurada.
     */
    @Transactional
    public AssistantResponse answer(AppUser user, String question) {
        String safeQuestion = question == null ? "" : question.trim();
        AssistantResponse response = findFaqAnswer(safeQuestion)
            .or(() -> findDynamicApplicationAnswer(safeQuestion))
            .orElseGet(() -> askGeminiOrFallback(safeQuestion));
        assistantLogRepository.save(AssistantLog.builder()
            .user(user)
            .question(safeQuestion)
            .answer(response.getAnswer())
            .source(response.getSource())
            .createdAt(LocalDateTime.now())
            .build());
        return response;
    }

    /**
     * Intenta encontrar una respuesta local por coincidencia semántica simple.
     *
     * @param question pregunta del usuario.
     * @return respuesta si existe coincidencia.
     */
    private Optional<AssistantResponse> findFaqAnswer(String question) {
        String normalizedQuestion = normalize(question);
        List<Faq> faqs = faqRepository.findByActiveTrue();
        return faqs.stream()
            .filter(faq -> normalizedQuestion.contains(normalize(faq.getQuestion())) || normalize(faq.getQuestion()).contains(normalizedQuestion))
            .findFirst()
            .map(faq -> new AssistantResponse(faq.getAnswer(), "FAQ_LOCAL"));
    }

    /**
     * Responde con datos internos cuando la pregunta pide información que está en la base de datos.
     *
     * @param question pregunta del usuario.
     * @return respuesta basada en BBDD si aplica.
     */
    private Optional<AssistantResponse> findDynamicApplicationAnswer(String question) {
        String normalized = normalize(question);
        if (containsAny(normalized, "stock", "producto", "productos", "tienda", "precio", "camiseta", "bufanda", "taza", "pack")) {
            return Optional.of(new AssistantResponse(buildProductAnswer(), "DATOS_APP_TIENDA"));
        }
        if (containsAny(normalized, "partido", "partidos", "entrada", "entradas", "calendario", "rival", "jugar")) {
            return Optional.of(new AssistantResponse(buildMatchAnswer(), "DATOS_APP_PARTIDOS"));
        }
        if (containsAny(normalized, "jugador", "jugadores", "plantilla", "dorsal")) {
            return Optional.of(new AssistantResponse(buildPlayerAnswer(), "DATOS_APP_JUGADORES"));
        }
        return Optional.empty();
    }

    /**
     * Genera una respuesta textual con productos, precios y stock actual.
     *
     * @return respuesta de tienda.
     */
    private String buildProductAnswer() {
        List<Product> products = productRepository.findByActiveTrue();
        if (products.isEmpty()) {
            return "Ahora mismo no hay productos activos en la tienda.";
        }
        StringBuilder answer = new StringBuilder("Estos son los productos activos de la tienda y su stock actual:\n\n");
        products.forEach(product -> answer.append("- ")
            .append(product.getName())
            .append(" · ")
            .append(product.getPrice())
            .append(" € · Stock: ")
            .append(product.getStock())
            .append(" unidades\n"));
        answer.append("\nPuedes abrir la tienda desde el menú superior para ver el detalle y hacer una compra simulada.");
        return answer.toString();
    }

    /**
     * Genera una respuesta textual con próximos partidos y entradas disponibles.
     *
     * @return respuesta de partidos.
     */
    private String buildMatchAnswer() {
        List<FootballMatch> matches = footballMatchRepository.findByMatchDateAfterOrderByMatchDateAsc(LocalDateTime.now());
        if (matches.isEmpty()) {
            return "Ahora mismo no hay partidos próximos cargados en la aplicación.";
        }
        StringBuilder answer = new StringBuilder("Estos son los partidos próximos cargados en El Plantío 360:\n\n");
        matches.forEach(match -> answer.append("- Burgos CF vs ")
            .append(match.getRival())
            .append(" · ")
            .append(match.getCompetition())
            .append(" · ")
            .append(match.getMatchDate() == null ? "fecha pendiente" : match.getMatchDate().format(DATE_FORMATTER))
            .append(" · Entradas disponibles: ")
            .append(match.getAvailableTickets())
            .append(" · Desde ")
            .append(match.getBasePrice())
            .append(" €\n"));
        answer.append("\nLos datos son simulados para la práctica y se pueden mantener desde el panel de administración.");
        return answer.toString();
    }

    /**
     * Genera una respuesta textual con jugadores cargados en la plantilla.
     *
     * @return respuesta de plantilla.
     */
    private String buildPlayerAnswer() {
        List<Player> players = playerRepository.findAllByOrderByDorsalAsc();
        if (players.isEmpty()) {
            return "No hay jugadores cargados actualmente en la plantilla.";
        }
        StringBuilder answer = new StringBuilder("Plantilla cargada actualmente:\n\n");
        players.forEach(player -> answer.append("- #")
            .append(player.getDorsal())
            .append(" ")
            .append(player.getName())
            .append(" · ")
            .append(player.getPosition())
            .append(" · Estado: ")
            .append(player.getStatus())
            .append("\n"));
        return answer.toString();
    }

    /**
     * Consulta Gemini cuando está configurado; si no, responde con fallback defendible.
     *
     * @param question pregunta del usuario.
     * @return respuesta del asistente.
     */
    private AssistantResponse askGeminiOrFallback(String question) {
        if (!geminiEnabled) {
            return new AssistantResponse("El modo Gemini está desactivado por configuración. Puedo ayudarte con la FAQ local, entradas, tienda, jugadores, mapa y partidos.", "FALLBACK_LOCAL");
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return new AssistantResponse("Gemini está habilitado, pero falta PLANTIO_GEMINI_API_KEY. Añade la clave al archivo .env y reinicia docker compose para usar IA real.", "GEMINI_PENDING_KEY");
        }
        try {
            String groundedPrompt = """
                Contexto interno actual de El Plantío 360:
                %s

                Pregunta del usuario:
                %s
                """.formatted(buildApplicationContext(), question);
            Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_CONTEXT))),
                "contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", groundedPrompt))
                )),
                "generationConfig", Map.of(
                    "temperature", 0.15,
                    "topP", 0.75,
                    "maxOutputTokens", 700
                )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = GEMINI_BASE_URL.formatted(geminiModel, geminiApiKey);
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = restTemplate.postForObject(url, new HttpEntity<>(payload, headers), Map.class);
            String answer = extractGeminiText(raw);
            return new AssistantResponse(answer, "GEMINI_" + geminiModel);
        } catch (RuntimeException ex) {
            return new AssistantResponse("El asistente IA no está disponible ahora mismo. La aplicación sigue funcionando con FAQ local, entradas, tienda, mapa y partidos.", "GEMINI_ERROR");
        }
    }

    /**
     * Construye un contexto breve con datos vivos para reducir alucinaciones del LLM.
     *
     * @return contexto de aplicación.
     */
    private String buildApplicationContext() {
        StringBuilder context = new StringBuilder();
        context.append("PRODUCTOS ACTIVOS:\n");
        productRepository.findByActiveTrue().forEach(product -> context.append("- ")
            .append(product.getName()).append(" | precio ").append(product.getPrice())
            .append(" € | stock ").append(product.getStock()).append(" | categoría ").append(product.getCategory()).append("\n"));
        context.append("\nPARTIDOS PRÓXIMOS:\n");
        footballMatchRepository.findByMatchDateAfterOrderByMatchDateAsc(LocalDateTime.now()).forEach(match -> context.append("- Burgos CF vs ")
            .append(match.getRival()).append(" | ").append(match.getCompetition())
            .append(" | ").append(match.getMatchDate() == null ? "fecha pendiente" : match.getMatchDate().format(DATE_FORMATTER))
            .append(" | entradas ").append(match.getAvailableTickets())
            .append(" | precio base ").append(match.getBasePrice()).append(" €\n"));
        context.append("\nJUGADORES:\n");
        playerRepository.findAllByOrderByDorsalAsc().forEach(player -> context.append("- #")
            .append(player.getDorsal()).append(" ").append(player.getName())
            .append(" | ").append(player.getPosition()).append(" | estado ").append(player.getStatus()).append("\n"));
        context.append("\nFUNCIONALIDADES: compra simulada, MailHog para correos, RabbitMQ para eventos, Redis para caché, Flask para sensores simulados, Leaflet/OpenStreetMap para mapa, WebSockets para muro.\n");
        return context.toString();
    }

    /**
     * Extrae el texto de la respuesta JSON de Gemini.
     *
     * @param raw respuesta JSON convertida a mapa.
     * @return texto final para mostrar al usuario.
     */
    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return "Gemini no ha devuelto contenido para esta pregunta.";
        }
        Object candidatesObject = raw.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            return "Gemini no ha devuelto candidatos de respuesta.";
        }
        Object firstCandidateObject = candidates.getFirst();
        if (!(firstCandidateObject instanceof Map<?, ?> firstCandidate)) {
            return "No he podido interpretar la respuesta de Gemini.";
        }
        Object contentObject = firstCandidate.get("content");
        if (!(contentObject instanceof Map<?, ?> content)) {
            return "Gemini ha respondido sin contenido textual.";
        }
        Object partsObject = content.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            return "Gemini ha respondido sin partes de texto.";
        }
        Object firstPartObject = parts.getFirst();
        if (!(firstPartObject instanceof Map<?, ?> firstPart)) {
            return "No he podido interpretar el texto de Gemini.";
        }
        Object textObject = firstPart.get("text");
        String text = textObject == null ? "" : textObject.toString().trim();
        return text.isBlank() ? "Gemini ha devuelto una respuesta vacía." : text;
    }

    /**
     * Indica si el texto contiene alguno de los términos de interés.
     *
     * @param text texto normalizado.
     * @param terms términos a comprobar.
     * @return true si alguno aparece.
     */
    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Normaliza texto para búsquedas tolerantes a mayúsculas y acentos.
     *
     * @param text texto de entrada.
     * @return texto normalizado.
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim();
    }
}
