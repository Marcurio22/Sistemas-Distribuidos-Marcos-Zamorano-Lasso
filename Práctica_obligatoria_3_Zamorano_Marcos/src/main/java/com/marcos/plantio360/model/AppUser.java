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
import java.time.LocalDateTime;

/**
 * Usuario de la plataforma con rol de seguridad.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class AppUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String firstName;
    @Column(nullable = false, length = 120)
    private String lastName;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, length = 30)
    private String role;
    @Column(length = 30)
    private String phone;
    @Column(nullable = false)
    private boolean enabled;
    @Column(length = 500)
    private String avatarUrl;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Inicializa campos por defecto antes de insertar.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (role == null || role.isBlank()) role = "ROLE_USER";
    }

    /**
     * Devuelve el nombre completo del usuario.
     *
     * @return nombre y apellidos.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
