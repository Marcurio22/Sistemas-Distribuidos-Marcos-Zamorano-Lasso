/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración de cliente HTTP para integraciones Spring-Flask y Gemini.
 */
@Configuration
public class WebClientConfig {
    /**
     * Devuelve un RestTemplate reutilizable para llamadas HTTP salientes.
     *
     * @return cliente HTTP síncrono.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
