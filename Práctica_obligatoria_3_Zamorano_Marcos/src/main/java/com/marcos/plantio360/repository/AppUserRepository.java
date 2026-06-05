/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.repository;

import com.marcos.plantio360.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
/** Repositorio JPA de usuarios. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    /** Busca usuario por email. */
    Optional<AppUser> findByEmail(String email);
    /** Comprueba si existe email. */
    boolean existsByEmail(String email);

    /** Comprueba si existe email en otro usuario. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
