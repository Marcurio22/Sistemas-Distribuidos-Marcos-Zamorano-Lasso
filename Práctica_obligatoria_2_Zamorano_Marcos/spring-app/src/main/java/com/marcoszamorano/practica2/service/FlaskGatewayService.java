package com.marcoszamorano.practica2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoszamorano.practica2.dto.ApiErrorResponse;
import com.marcoszamorano.practica2.dto.ApiSuccessResponse;
import com.marcoszamorano.practica2.exception.RemoteServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

@Service
public class FlaskGatewayService {

    /**
     * Esta clase encapsula toda la comunicación HTTP entre Spring y Flask.
     *
     * Su responsabilidad es:
     * - construir las URLs de Flask
     * - invocar los endpoints
     * - interpretar respuestas correctas
     * - transformar errores HTTP, timeout o conectividad en excepciones propias
     */
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String pythonBaseUrl;

    public FlaskGatewayService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${app.python.base-url}") String pythonBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.pythonBaseUrl = pythonBaseUrl;
    }

    public ApiSuccessResponse readFile(String filename) {
        String safeFilename = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
        String url = pythonBaseUrl + "/api/lab/files/read/" + safeFilename;
        return performGet(url);
    }

    public ApiSuccessResponse listUsers() {
        String url = pythonBaseUrl + "/api/lab/database/users";
        return performGet(url);
    }

    public ApiSuccessResponse forceDatabaseError() {
        String url = pythonBaseUrl + "/api/lab/database/force-error";
        return performGet(url);
    }

    public ApiSuccessResponse getPokemon(String name) {
        String safeName = UriUtils.encodePathSegment(name, StandardCharsets.UTF_8);
        String url = pythonBaseUrl + "/api/lab/pokemon/" + safeName;
        return performGet(url);
    }

    public ApiSuccessResponse simulateTimeout(int seconds) {
        String url = UriComponentsBuilder
                .fromHttpUrl(pythonBaseUrl + "/api/lab/network/timeout")
                .queryParam("seconds", seconds)
                .toUriString();

        return performGet(url);
    }

    /**
     * Método común para todas las llamadas GET al servicio Flask.
     *
     * Flujo:
     * - si la respuesta es correcta, devuelve su body
     * - si Flask responde con error HTTP, se parsea su JSON de error
     * - si hay timeout, se lanza una excepción específica
     * - si Flask no está disponible, se informa como servicio remoto caído
     */
    private ApiSuccessResponse performGet(String url) {
        try {
            ResponseEntity<ApiSuccessResponse> response =
                    restTemplate.getForEntity(url, ApiSuccessResponse.class);

            if (response.getBody() == null) {
                throw RemoteServiceException.unexpected("La API Flask ha devuelto una respuesta vacía.");
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            throw new RemoteServiceException(parseRemoteError(ex));

        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw RemoteServiceException.timeout(ex.getMessage());
            }
            throw RemoteServiceException.unavailable(ex.getMessage());

        } catch (RemoteServiceException ex) {
            throw ex;

        } catch (Exception ex) {
            throw RemoteServiceException.unexpected(ex.getMessage());
        }
    }

    /**
     * Intenta distinguir si un ResourceAccessException corresponde realmente
     * a un timeout de lectura/conexión.
     */
    private boolean isTimeout(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        String message = ex.getMessage();
        return message != null && message.toLowerCase().contains("timed out");
    }

    /**
     * Convierte el JSON de error devuelto por Flask en ApiErrorResponse.
     *
     * Si no se puede parsear correctamente, se construye un error de respaldo
     * para no perder información y mantener el flujo de tratamiento homogéneo.
     */
    private ApiErrorResponse parseRemoteError(HttpStatusCodeException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                ApiErrorResponse parsed = objectMapper.readValue(body, ApiErrorResponse.class);
                if (parsed.getHttpStatus() == null) {
                    parsed.setHttpStatus(ex.getStatusCode().value());
                }
                return parsed;
            }
        } catch (Exception ignored) {
        }

        ApiErrorResponse fallback = new ApiErrorResponse();
        fallback.setSuccess(false);
        fallback.setSource("python-api");
        fallback.setCategory("REMOTE_HTTP_ERROR");
        fallback.setErrorCode("UNPARSEABLE_REMOTE_ERROR");
        fallback.setHttpStatus(ex.getStatusCode().value());
        fallback.setUserMessage("El servicio Flask ha devuelto un error HTTP no interpretable.");
        fallback.setTechnicalMessage(ex.getResponseBodyAsString());
        fallback.setCritical(true);
        fallback.setPath("remote-call");
        fallback.setTimestamp(java.time.OffsetDateTime.now().toString());
        return fallback;
    }
}