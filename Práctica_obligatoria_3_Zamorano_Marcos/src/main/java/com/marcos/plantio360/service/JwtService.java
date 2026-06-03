/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Servicio JWT manual HMAC-SHA256 sin dependencia externa.
 */
@Service
public class JwtService {
    private final String secret;
    private final long expirationSeconds;

    /**
     * Crea el servicio JWT.
     *
     * @param secret clave de firma.
     * @param expirationSeconds duración del token.
     */
    public JwtService(@Value("${plantio.jwt.secret}") String secret,
                      @Value("${plantio.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Genera JWT firmado.
     *
     * @param subject email del usuario.
     * @param role rol del usuario.
     * @return token compacto.
     */
    public String generateToken(String subject, String role) {
        long now = Instant.now().getEpochSecond();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + escape(subject) + "\",\"role\":\"" + escape(role) + "\",\"iat\":" + now + ",\"exp\":" + (now + expirationSeconds) + "}");
        String unsigned = header + "." + payload;
        return unsigned + "." + sign(unsigned);
    }

    /**
     * Extrae sujeto del token.
     *
     * @param token token JWT.
     * @return email o null.
     */
    public String extractSubject(String token) {
        return parsePayload(token).get("sub");
    }

    /**
     * Valida firma, expiración y sujeto.
     *
     * @param token token JWT.
     * @param expectedSubject sujeto esperado.
     * @return true si es válido.
     */
    public boolean isTokenValid(String token, String expectedSubject) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String unsigned = parts[0] + "." + parts[1];
            if (!sign(unsigned).equals(parts[2])) return false;
            Map<String, String> payload = parsePayload(token);
            long exp = Long.parseLong(payload.getOrDefault("exp", "0"));
            return expectedSubject.equals(payload.get("sub")) && exp > Instant.now().getEpochSecond();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** Codifica texto base64url. */
    private String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Firma contenido con HMAC-SHA256. */
    private String sign(String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar JWT", ex);
        }
    }

    /** Escapa comillas simples del JSON mínimo. */
    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    /** Parsea payload JSON simple a mapa. */
    private Map<String, String> parsePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) return Map.of();
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, String> values = new HashMap<>();
        String body = json.substring(1, json.length() - 1);
        for (String pair : body.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) values.put(kv[0].replace("\"", "").trim(), kv[1].replace("\"", "").trim());
        }
        return values;
    }
}
