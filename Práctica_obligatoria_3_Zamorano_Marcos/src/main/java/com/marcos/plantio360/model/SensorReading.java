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
import java.time.LocalDateTime;

/**
 * Lectura de remote sensing simulada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sensor_readings")
public class SensorReading implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 50)
    private String type;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    @Column(nullable = false)
    private LocalDateTime capturedAt;

    /**
     * Marca fecha de captura por defecto.
     */
    @PrePersist
    public void prePersist() {
        if (capturedAt == null) capturedAt = LocalDateTime.now();
    }
}
