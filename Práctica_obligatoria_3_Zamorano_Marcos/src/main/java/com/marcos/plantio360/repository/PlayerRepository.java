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
/** Repositorio JPA de jugadores. */
public interface PlayerRepository extends JpaRepository<Player, Long> {
    /** Devuelve jugadores ordenados por dorsal. */
    List<Player> findAllByOrderByDorsalAsc();

    /** Comprueba si un dorsal ya está asignado. */
    boolean existsByDorsal(Integer dorsal);

    /** Comprueba si un dorsal está asignado a otro jugador distinto del indicado. */
    boolean existsByDorsalAndIdNot(Integer dorsal, Long id);
}
