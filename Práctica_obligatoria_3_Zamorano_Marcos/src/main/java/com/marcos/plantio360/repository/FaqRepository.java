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
/** Repositorio JPA de FAQs. */
public interface FaqRepository extends JpaRepository<Faq, Long> {
    /** Devuelve FAQs activas. */
    List<Faq> findByActiveTrue();

    /** Busca una FAQ por su pregunta exacta. */
    Optional<Faq> findByQuestion(String question);
}
