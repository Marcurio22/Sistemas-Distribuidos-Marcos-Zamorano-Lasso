/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.repository;

import com.marcos.plantio360.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
/** Repositorio JPA de notificaciones. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /** Devuelve notificaciones de usuario. */
    List<Notification> findByUserOrderByCreatedAtDesc(AppUser user);
}
