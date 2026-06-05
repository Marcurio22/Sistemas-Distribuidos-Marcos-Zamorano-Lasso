/**
 * El Plantío 360 - Formulario administrativo de jugador.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import com.marcos.plantio360.model.Player;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO con campos de texto para controlar manualmente conversiones y mensajes de validación.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlayerForm {
    private String id;
    private String dorsal;
    private String name;
    private String position;
    private String nationality;
    private String status;
    private String age;
    private String height;
    private String weight;
    private String goals;
    private String assists;
    private String description;
    private String imageUrl;

    /** Crea un formulario inicial de alta de jugador. */
    public static AdminPlayerForm empty() {
        return AdminPlayerForm.builder()
            .position("Delantero")
            .nationality("España")
            .status("DISPONIBLE")
            .goals("0")
            .assists("0")
            .imageUrl("/images/avatar.svg")
            .build();
    }

    /** Convierte una entidad persistida en DTO de formulario. */
    public static AdminPlayerForm from(Player player) {
        return AdminPlayerForm.builder()
            .id(value(player.getId()))
            .dorsal(value(player.getDorsal()))
            .name(player.getName())
            .position(player.getPosition())
            .nationality(player.getNationality())
            .status(player.getStatus())
            .age(value(player.getAge()))
            .height(decimal(player.getHeight()))
            .weight(decimal(player.getWeight()))
            .goals(value(player.getGoals()))
            .assists(value(player.getAssists()))
            .description(player.getDescription())
            .imageUrl(player.getImageUrl())
            .build();
    }

    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
