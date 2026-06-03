/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Datos de checkout para pasarela de pago simulada.
 */
@Getter
@Setter
public class CheckoutRequest {
    private String type;
    private Long itemId;
    private Integer quantity = 1;
    private String zone;
    private String cardHolder;
    private String cardNumber;
    private String expiry;
    private String cvv;
}
