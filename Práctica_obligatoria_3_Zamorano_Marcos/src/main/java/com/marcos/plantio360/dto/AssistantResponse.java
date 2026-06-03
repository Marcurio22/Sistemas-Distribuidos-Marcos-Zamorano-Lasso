/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta del asistente inteligente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssistantResponse {
    private String answer;
    private String source;
}
