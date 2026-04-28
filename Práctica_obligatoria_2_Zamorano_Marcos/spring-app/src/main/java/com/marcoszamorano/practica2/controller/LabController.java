package com.marcoszamorano.practica2.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoszamorano.practica2.dto.ApiSuccessResponse;
import com.marcoszamorano.practica2.dto.LabResponseView;
import com.marcoszamorano.practica2.dto.lab.PokemonDetailsView;
import com.marcoszamorano.practica2.dto.lab.PokemonStatView;
import com.marcoszamorano.practica2.exception.RemoteServiceException;
import com.marcoszamorano.practica2.service.ErrorTranslatorService;
import com.marcoszamorano.practica2.service.FlaskGatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Controller
@RequestMapping("/lab")
public class LabController {

    /**
     * Este controlador es el núcleo del laboratorio web.
     * Desde aquí se reciben las acciones del usuario en /lab,
     * se invoca a Flask mediante FlaskGatewayService y se construye
     * una vista final adaptada a Thymeleaf.
     */
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

    /**
     * Carga inicial de la pantalla del laboratorio.
     * No muestra resultados todavía, solo la interfaz de pruebas.
     */
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

    /**
     * Método común que centraliza el patrón de ejecución del laboratorio:
     * - invocar Flask
     * - si hay éxito, construir la respuesta visual de éxito
     * - si hay error remoto, traducirlo
     * - si ocurre algo inesperado, generar un error interno controlado
     */
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

    /**
     * Construye el objeto que la vista Thymeleaf utilizará para representar
     * un caso correcto del laboratorio.
     */
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

        if ("get_pokemon".equals(response.getOperation())) {
            view.setPokemonDetails(mapPokemonDetails(response.getData()));
        }

        return view;
    }

    /**
     * Convierte el Map genérico recibido desde Flask en un modelo tipado
     * para la ficha visual del Pokémon.
     */
    private PokemonDetailsView mapPokemonDetails(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        PokemonDetailsView details = new PokemonDetailsView();
        details.setId(asInteger(data.get("id")));
        details.setName(asString(data.get("name")));
        details.setHeight(asInteger(data.get("height")));
        details.setWeight(asInteger(data.get("weight")));
        details.setBaseExperience(asInteger(data.get("base_experience")));
        details.setTypes(asStringList(data.get("types")));
        details.setAbilities(asStringList(data.get("abilities")));

        Object spritesObj = data.get("sprites");
        if (spritesObj instanceof Map<?, ?> spritesMap) {
            details.setOfficialArtwork(asString(spritesMap.get("official_artwork")));
            details.setFrontDefault(asString(spritesMap.get("front_default")));
        }

        details.setStats(asPokemonStats(data.get("stats")));
        return details;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }

    private List<String> asStringList(Object value) {
        List<String> result = new ArrayList<>();

        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
        }

        return result;
    }

    /**
     * Mapea la lista de estadísticas del Pokémon a una representación
     * específica de la vista, que incluye además porcentaje para barras.
     */
    private List<PokemonStatView> asPokemonStats(Object value) {
        List<PokemonStatView> result = new ArrayList<>();

        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> statMap) {
                    String statName = asString(statMap.get("name"));
                    Integer statValue = asInteger(statMap.get("value"));

                    if (statName != null && statValue != null) {
                        result.add(new PokemonStatView(statName, statValue));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Convierte cualquier estructura a JSON para mostrarla en el
     * laboratorio como información técnica secundaria.
     */
    private String prettyJson(Object data) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }
}