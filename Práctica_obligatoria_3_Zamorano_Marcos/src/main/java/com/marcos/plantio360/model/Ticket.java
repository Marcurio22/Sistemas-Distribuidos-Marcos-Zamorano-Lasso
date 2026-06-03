/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.model;

import java.io.Serializable;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada individual para un partido.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private FootballMatch match;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser owner;
    @Column(nullable = false, length = 80)
    private String zone;
    @Column(nullable = false, length = 30)
    private String seatCode;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(length = 300)
    private String qrCode;
    private LocalDateTime soldAt;

    /**
     * Marca la entrada como disponible al crearla.
     */
    @PrePersist
    public void prePersist() {
        if (status == null) status = "AVAILABLE";
    }
}
