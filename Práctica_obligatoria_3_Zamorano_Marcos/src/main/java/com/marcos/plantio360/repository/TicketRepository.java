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
/** Repositorio JPA de entradas. */
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    /** Devuelve entradas de un partido en estado concreto. */
    List<Ticket> findByMatchAndStatus(FootballMatch match, String status);
    /** Devuelve entradas de un usuario. */
    List<Ticket> findByOwner(AppUser owner);
}
