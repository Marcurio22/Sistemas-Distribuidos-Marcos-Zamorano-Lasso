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

/**
 * Pregunta frecuente del asistente IA.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "faqs")
public class Faq implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 300)
    private String question;
    @Column(nullable = false, length = 4000)
    private String answer;
    @Column(nullable = false, length = 120)
    private String category;
    @Column(nullable = false)
    private boolean active;

    /**
     * Activa la FAQ por defecto.
     */
    @PrePersist
    public void prePersist() {
        active = true;
    }
}
