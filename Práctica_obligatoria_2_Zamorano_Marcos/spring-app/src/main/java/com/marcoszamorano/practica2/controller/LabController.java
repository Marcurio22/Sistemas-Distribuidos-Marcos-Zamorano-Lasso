package com.marcoszamorano.practica2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoszamorano.practica2.dto.ApiSuccessResponse;
import com.marcoszamorano.practica2.dto.LabResponseView;
import com.marcoszamorano.practica2.exception.RemoteServiceException;
import com.marcoszamorano.practica2.service.ErrorTranslatorService;
import com.marcoszamorano.practica2.service.FlaskGatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.function.Supplier;

@Controller
@RequestMapping("/lab")
public class LabController {

    private final FlaskGatewayService flaskGatewayService;
    private final ErrorTranslatorService errorTranslatorService;
    private final ObjectMapper objectMapper;

    public LabController(
            FlaskGatewayService flaskGatewayService,
            ErrorTranslatorService errorTranslatorService,
            ObjectMapper objectMapper
    ) {
        this.flaskGatewayService = flaskGatewayService;
        this.errorTranslatorService = errorTranslatorService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String labHome(Model model) {
        model.addAttribute("result", null);
        return "lab";
    }

    @GetMapping("/files/read")
    public String readFile(@RequestParam String filename, Model model) {
        return execute("Lectura de fichero: " + filename, () -> flaskGatewayService.readFile(filename), model);
    }

    @GetMapping("/database/users")
    public String listUsers(Model model) {
        return execute("Consulta de usuarios en base de datos", flaskGatewayService::listUsers, model);
    }

    @GetMapping("/database/force-error")
    public String forceDatabaseError(Model model) {
        return execute("Error forzado de base de datos", flaskGatewayService::forceDatabaseError, model);
    }

    @GetMapping("/pokemon")
    public String getPokemon(@RequestParam String name, Model model) {
        return execute("Consulta de Pokémon: " + name, () -> flaskGatewayService.getPokemon(name), model);
    }

    @GetMapping("/network/timeout")
    public String timeout(@RequestParam(defaultValue = "10") int seconds, Model model) {
        return execute("Simulación de timeout (" + seconds + " s)", () -> flaskGatewayService.simulateTimeout(seconds), model);
    }

    private String execute(String operationLabel, Supplier<ApiSuccessResponse> action, Model model) {
        try {
            ApiSuccessResponse response = action.get();
            model.addAttribute("result", buildSuccessView(operationLabel, response));
        } catch (RemoteServiceException ex) {
            model.addAttribute("result", errorTranslatorService.translate(ex, operationLabel));
        } catch (Exception ex) {
            model.addAttribute("result", errorTranslatorService.translate(
                    RemoteServiceException.unexpected(ex.getMessage()),
                    operationLabel
            ));
        }

        return "lab";
    }

    private LabResponseView buildSuccessView(String operationLabel, ApiSuccessResponse response) {
        LabResponseView view = new LabResponseView();
        view.setSuccess(true);
        view.setOperationLabel(operationLabel);
        view.setUserMessage("La operación se ha ejecutado correctamente.");
        view.setTechnicalMessage("Respuesta recibida correctamente desde Flask.");
        view.setErrorCode("SIN_ERROR");
        view.setCategory("SUCCESS");
        view.setHttpStatus(200);
        view.setCritical(false);
        view.setTimestamp(response.getTimestamp());
        view.setRawDataJson(prettyJson(response.getData()));
        return view;
    }

    private String prettyJson(Object data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }
}