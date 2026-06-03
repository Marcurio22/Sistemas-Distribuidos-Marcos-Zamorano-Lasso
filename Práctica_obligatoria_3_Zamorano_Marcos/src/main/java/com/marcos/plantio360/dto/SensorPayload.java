/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** Payload procedente del microservicio Flask. */
@Getter
@Setter
public class SensorPayload { private String name; private String type; private BigDecimal value; private String unit; private String status; private BigDecimal latitude; private BigDecimal longitude; }
