/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declaración de colas RabbitMQ.
 */
@Configuration
public class RabbitMqConfig {
    public static final String PURCHASE_QUEUE = "plantio.purchase.created";
    public static final String EMAIL_QUEUE = "plantio.email.pending";

    /** Crea cola de compras. */
    @Bean
    public Queue purchaseQueue() { return new Queue(PURCHASE_QUEUE, true); }

    /** Crea cola de emails. */
    @Bean
    public Queue emailQueue() { return new Queue(EMAIL_QUEUE, true); }
}
