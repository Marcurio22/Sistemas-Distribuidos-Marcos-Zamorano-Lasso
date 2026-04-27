package com.marcoszamorano.practica2.exception;

import com.marcoszamorano.practica2.dto.ApiErrorResponse;

public class RemoteServiceException extends RuntimeException {

    private final ApiErrorResponse errorResponse;

    public RemoteServiceException(ApiErrorResponse errorResponse) {
        super(errorResponse != null ? errorResponse.getTechnicalMessage() : "Error remoto desconocido");
        this.errorResponse = errorResponse;
    }

    public ApiErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public static RemoteServiceException timeout(String technicalMessage) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setSuccess(false);
        error.setSource("spring-client");
        error.setCategory("NETWORK_ERROR");
        error.setErrorCode("CLIENT_TIMEOUT");
        error.setHttpStatus(504);
        error.setUserMessage("El servicio remoto ha tardado demasiado en responder.");
        error.setTechnicalMessage(technicalMessage);
        error.setCritical(true);
        error.setPath("remote-call");
        error.setTimestamp(java.time.OffsetDateTime.now().toString());
        return new RemoteServiceException(error);
    }

    public static RemoteServiceException unavailable(String technicalMessage) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setSuccess(false);
        error.setSource("spring-client");
        error.setCategory("NETWORK_ERROR");
        error.setErrorCode("REMOTE_SERVICE_UNAVAILABLE");
        error.setHttpStatus(503);
        error.setUserMessage("No se ha podido contactar con el servicio Flask.");
        error.setTechnicalMessage(technicalMessage);
        error.setCritical(true);
        error.setPath("remote-call");
        error.setTimestamp(java.time.OffsetDateTime.now().toString());
        return new RemoteServiceException(error);
    }

    public static RemoteServiceException unexpected(String technicalMessage) {
        ApiErrorResponse error = new ApiErrorResponse();
        error.setSuccess(false);
        error.setSource("spring-client");
        error.setCategory("INTERNAL_ERROR");
        error.setErrorCode("SPRING_UNEXPECTED_ERROR");
        error.setHttpStatus(500);
        error.setUserMessage("Se ha producido un error inesperado al procesar la respuesta remota.");
        error.setTechnicalMessage(technicalMessage);
        error.setCritical(true);
        error.setPath("remote-call");
        error.setTimestamp(java.time.OffsetDateTime.now().toString());
        return new RemoteServiceException(error);
    }
}