/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import lombok.Getter;
import lombok.Setter;

/** Payload de chat WebSocket. */
@Getter
@Setter
public class ChatPayload {
    private String content;
    private String displayName;
}
