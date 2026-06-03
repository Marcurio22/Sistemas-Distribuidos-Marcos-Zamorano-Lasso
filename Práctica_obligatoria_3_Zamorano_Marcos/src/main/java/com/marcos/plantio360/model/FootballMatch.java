/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.model;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partido programado en la plataforma.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "matches")
public class FootballMatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String rival;
    @Column(nullable = false, length = 120)
    private String competition;
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime matchDate;
    @Column(nullable = false, length = 120)
    private String stadium;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    @Column(nullable = false)
    private Integer availableTickets;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(length = 600)
    private String imageUrl;

    /**
     * Inicializa estado y disponibilidad.
     */
    @PrePersist
    public void prePersist() {
        if (status == null) status = "SCHEDULED";
        if (availableTickets == null) availableTickets = 0;
    }
}
