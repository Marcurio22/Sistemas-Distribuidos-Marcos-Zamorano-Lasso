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
 * Producto de merchandising de la tienda.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private Integer stock;
    @Column(nullable = false, length = 40)
    private String category;
    @Column(length = 600)
    private String imageUrl;
    @Column(nullable = false)
    private boolean active;

    /**
     * Activa por defecto el producto.
     */
    @PrePersist
    public void prePersist() {
        active = true;
    }
}
