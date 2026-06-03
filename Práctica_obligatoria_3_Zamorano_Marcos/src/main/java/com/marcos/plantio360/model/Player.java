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
import java.math.BigDecimal;

/**
 * Jugador ficticio de la plantilla blanquinegra.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "players")
public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private Integer dorsal;
    @Column(nullable = false, length = 80)
    private String position;
    @Column(nullable = false, length = 80)
    private String nationality;
    private Integer age;
    private BigDecimal height;
    private BigDecimal weight;
    @Column(length = 600)
    private String imageUrl;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, length = 40)
    private String status;
    private Integer goals;
    private Integer assists;

    /**
     * Completa valores deportivos por defecto.
     */
    @PrePersist
    public void prePersist() {
        if (status == null) status = "AVAILABLE";
        if (goals == null) goals = 0;
        if (assists == null) assists = 0;
    }
}
