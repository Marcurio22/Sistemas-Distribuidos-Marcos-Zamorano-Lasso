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
import com.marcos.plantio360.repository.AssistantLogRepository;
import com.marcos.plantio360.repository.FaqRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio del asistente Blanquinegro Bot con FAQ local y fallback Gemini habilitable por entorno.
 */
@Service
@RequiredArgsConstructor
public class AssistantService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String SYSTEM_CONTEXT = """
        Eres Blanquinegro Bot, asistente de El Plantío 360, una plataforma académica para aficionados al fútbol.
        Responde siempre en español, con tono cercano, breve y útil.
        Puedes ayudar con entradas, partidos, jugadores, tienda, mapa del estadio, sensores, parking, normas y dudas generales.
        Si algo depende de datos reales del club, aclara que se trata de un prototipo académico con datos simulados.
        """;

    private final FaqRepository faqRepository;
    private final AssistantLogRepository assistantLogRepository;
    private final RestTemplate restTemplate;

    @Value("${plantio.gemini.api-key:${spring.ai.google.genai.api-key:}}")
    private String geminiApiKey;

    @Value("${plantio.gemini.enabled:true}")
    private boolean geminiEnabled;

    @Value("${plantio.gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    /**
     * Responde una pregunta buscando primero en la FAQ local y consultando Gemini si está habilitado.
     *
     * @param user usuario que pregunta.
     * @param question pregunta textual.
     * @return respuesta estructurada.
     */
    @Transactional
    public AssistantResponse answer(AppUser user, String question) {
        String safeQuestion = question == null ? "" : question.trim();
        AssistantResponse response = findFaqAnswer(safeQuestion)
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
            Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_CONTEXT))),
                "contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", question))
                )),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "topP", 0.9,
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
