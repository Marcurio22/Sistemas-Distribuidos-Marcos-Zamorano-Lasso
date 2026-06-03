/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/**
 * Consumidor RabbitMQ que simula emails transaccionales mediante MailHog.
 */
@Service
@RequiredArgsConstructor
public class EmailConsumerService {
    private final MailSender mailSender;

    /**
     * Procesa eventos de compra y genera un email visible en MailHog.
     *
     * @param payload mensaje del evento.
     */
    @RabbitListener(queues = "plantio.purchase.created")
    public void onPurchaseCreated(String payload) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@plantio360.local");
        message.setTo("profesor@plantio360.local");
        message.setSubject("El Plantío 360 - Evento de compra");
        message.setText("Evento procesado asíncronamente desde RabbitMQ:\n\n" + payload + "\n\nRevisa MailHog para comprobar el flujo completo.");
        mailSender.send(message);
    }
}
