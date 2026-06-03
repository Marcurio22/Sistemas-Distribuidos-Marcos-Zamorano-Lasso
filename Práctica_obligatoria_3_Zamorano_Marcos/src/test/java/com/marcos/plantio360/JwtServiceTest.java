/**
 * El Plantío 360 - Tests de JWT.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360;

import com.marcos.plantio360.service.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del servicio JWT manual.
 */
class JwtServiceTest {
    /**
     * Verifica generación, extracción y validación del token JWT.
     */
    @Test
    void shouldGenerateAndValidateJwt() {
        JwtService service = new JwtService("TestingSecretForPlantio360", 3600);
        String token = service.generateToken("user@plantio360.local", "ROLE_USER");
        Assertions.assertEquals("user@plantio360.local", service.extractSubject(token));
        Assertions.assertTrue(service.isTokenValid(token, "user@plantio360.local"));
        Assertions.assertFalse(service.isTokenValid(token, "other@plantio360.local"));
    }
}
