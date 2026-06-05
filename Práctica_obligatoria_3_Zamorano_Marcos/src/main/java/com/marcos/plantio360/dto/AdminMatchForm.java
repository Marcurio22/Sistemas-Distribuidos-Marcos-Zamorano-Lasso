/**
 * El Plantío 360 - Formulario administrativo de partido.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import com.marcos.plantio360.model.FootballMatch;
import lombok.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * DTO de partido con fecha preparada para input datetime-local.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMatchForm {
    private String id;
    private String rival;
    private String competition;
    private String matchDateValue;
    private String stadium;
    private String basePrice;
    private String availableTickets;
    private String status;
    @Builder.Default
    private Boolean homeGame = true;
    private String imageUrl;

    /** Crea un formulario inicial de alta de partido. */
    public static AdminMatchForm from(FootballMatch match, DateTimeFormatter htmlDateTime) {
        return AdminMatchForm.builder()
            .id(value(match.getId()))
            .rival(match.getRival())
            .competition(match.getCompetition())
            .matchDateValue(match.getMatchDate() == null ? null : match.getMatchDate().format(htmlDateTime))
            .stadium(match.getStadium())
            .basePrice(decimal(match.getBasePrice()))
            .availableTickets(value(match.getAvailableTickets()))
            .status(match.getStatus())
            .homeGame(match.getHomeGame() == null ? true : match.getHomeGame())
            .imageUrl(match.getImageUrl())
            .build();
    }

    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
