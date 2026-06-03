/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.repository;

import com.marcos.plantio360.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
/** Repositorio JPA de partidos. */
public interface FootballMatchRepository extends JpaRepository<FootballMatch, Long> {
    /** Devuelve próximos partidos. */
    List<FootballMatch> findByMatchDateAfterOrderByMatchDateAsc(LocalDateTime now);
}
