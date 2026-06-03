/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import lombok.Getter;
import lombok.Setter;

/** Datos de login MVC/REST. */
@Getter
@Setter
public class LoginRequest { private String email; private String password; }
