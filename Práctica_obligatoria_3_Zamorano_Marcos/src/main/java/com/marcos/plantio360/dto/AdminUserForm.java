/**
 * El Plantío 360 - Formulario administrativo de usuario.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.dto;

import com.marcos.plantio360.model.AppUser;
import lombok.*;

/**
 * DTO simple para evitar que el formulario admin escriba directamente sobre la entidad JPA.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserForm {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    @Builder.Default
    private Boolean enabled = true;
    private String avatarUrl;

    /** Crea un formulario inicial de alta de usuario. */
    public static AdminUserForm empty() {
        return AdminUserForm.builder()
            .role("ROLE_USER")
            .enabled(true)
            .avatarUrl("/images/avatar.svg")
            .build();
    }

    /** Convierte una entidad persistida en DTO de formulario. */
    public static AdminUserForm from(AppUser user) {
        return AdminUserForm.builder()
            .id(user.getId() == null ? null : String.valueOf(user.getId()))
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole())
            .enabled(user.isEnabled())
            .avatarUrl(user.getAvatarUrl())
            .build();
    }
}
