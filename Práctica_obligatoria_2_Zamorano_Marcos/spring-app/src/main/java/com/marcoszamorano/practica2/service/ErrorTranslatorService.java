package com.marcoszamorano.practica2.service;

import com.marcoszamorano.practica2.dto.ApiErrorResponse;
import com.marcoszamorano.practica2.dto.LabResponseView;
import com.marcoszamorano.practica2.exception.RemoteServiceException;
import org.springframework.stereotype.Service;

@Service
public class ErrorTranslatorService {

    public LabResponseView translate(RemoteServiceException ex, String operationLabel) {
        ApiErrorResponse error = ex.getErrorResponse();

        LabResponseView view = new LabResponseView();
        view.setSuccess(false);
        view.setOperationLabel(operationLabel);
        view.setUserMessage(resolveFriendlyMessage(error));
        view.setTechnicalMessage(error.getTechnicalMessage());
        view.setErrorCode(error.getErrorCode());
        view.setCategory(error.getCategory());
        view.setHttpStatus(error.getHttpStatus());
        view.setCritical(error.getCritical());
        view.setTimestamp(error.getTimestamp());
        view.setRawDataJson(null);

        return view;
    }

    private String resolveFriendlyMessage(ApiErrorResponse error) {
        if (error == null || error.getErrorCode() == null) {
            return "Se ha producido un error remoto no identificado.";
        }

        return switch (error.getErrorCode()) {
            case "FILE_NOT_FOUND" ->
                    "El fichero solicitado no existe. Es un error controlado y no crítico.";
            case "FILE_FORBIDDEN" ->
                    "Se ha simulado correctamente un acceso prohibido al fichero.";
            case "INVALID_FILE_NAME" ->
                    "El nombre del fichero no es válido.";
            case "FILE_READ_ERROR" ->
                    "Se ha producido un error crítico al leer el fichero.";
            case "DATABASE_READ_ERROR" ->
                    "La consulta de base de datos ha fallado en el servicio Python.";
            case "DATABASE_FORCED_ERROR" ->
                    "Se ha provocado correctamente un error de base de datos para demostrar el tratamiento de excepciones.";
            case "POKEMON_NOT_FOUND" ->
                    "El Pokémon solicitado no existe en la API externa.";
            case "EXTERNAL_API_ERROR" ->
                    "La llamada al servicio externo de Pokémon ha fallado.";
            case "EXTERNAL_API_TIMEOUT", "CLIENT_TIMEOUT" ->
                    "El servicio remoto ha tardado demasiado en responder y Spring ha agotado el tiempo de espera.";
            case "REMOTE_SERVICE_UNAVAILABLE" ->
                    "No ha sido posible contactar con el servicio Flask.";
            case "ROUTE_NOT_FOUND" ->
                    "La ruta invocada no existe en el servicio Flask.";
            default ->
                    error.getUserMessage() != null
                            ? error.getUserMessage()
                            : "Se ha producido un error remoto.";
        };
    }
}