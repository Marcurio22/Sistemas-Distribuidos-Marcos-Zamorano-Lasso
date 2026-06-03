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
import java.util.Optional;
/** Repositorio JPA de productos. */
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Devuelve productos activos. */
    List<Product> findByActiveTrue();

    /** Busca un producto por nombre exacto para permitir una carga inicial idempotente. */
    Optional<Product> findByName(String name);
}
