/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.dto.SensorPayload;
import com.marcos.plantio360.model.SensorReading;
import com.marcos.plantio360.repository.SensorReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de integración con el microservicio Flask de sensores simulados.
 */
@Service
@RequiredArgsConstructor
public class SensorService {
    private final SensorReadingRepository sensorReadingRepository;
    private final RestTemplate restTemplate;

    @Value("${plantio.python-api.base-url:http://python-api:5000}")
    private String pythonApiBaseUrl;

    /**
     * Sincroniza lecturas de sensores desde Flask y las persiste en MySQL.
     *
     * @return lecturas persistidas.
     */
    @Transactional
    public List<SensorReading> syncFromPythonApi() {
        SensorPayload[] response = restTemplate.getForObject(pythonApiBaseUrl + "/api/sensors", SensorPayload[].class);
        List<SensorPayload> payloads = response == null ? List.of() : List.of(response);
        if (payloads == null || payloads.isEmpty()) {
            return sensorReadingRepository.findTop20ByOrderByCapturedAtDesc();
        }
        List<SensorReading> readings = payloads.stream().map(this::toEntity).toList();
        return sensorReadingRepository.saveAll(readings);
    }

    /**
     * Devuelve las últimas lecturas de sensores conocidas.
     *
     * @return lecturas recientes.
     */
    @Transactional(readOnly = true)
    public List<SensorReading> latest() {
        return sensorReadingRepository.findTop20ByOrderByCapturedAtDesc();
    }

    /**
     * Convierte el DTO recibido por Flask en entidad JPA.
     *
     * @param payload sensor recibido.
     * @return entidad persistible.
     */
    private SensorReading toEntity(SensorPayload payload) {
        return SensorReading.builder()
            .name(payload.getName())
            .type(payload.getType())
            .value(payload.getValue() == null ? BigDecimal.ZERO : payload.getValue())
            .unit(payload.getUnit())
            .status(payload.getStatus())
            .latitude(payload.getLatitude())
            .longitude(payload.getLongitude())
            .capturedAt(LocalDateTime.now())
            .build();
    }
}
