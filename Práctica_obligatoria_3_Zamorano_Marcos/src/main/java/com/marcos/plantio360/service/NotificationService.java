/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.model.Notification;
import com.marcos.plantio360.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona notificaciones persistidas y enviadas por WebSocket.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Crea una notificación para un usuario y la publica en tiempo real.
     *
     * @param user usuario destinatario.
     * @param type tipo funcional.
     * @param title título visible.
     * @param message mensaje visible.
     * @return notificación persistida.
     */
    @Transactional
    public Notification notifyUser(AppUser user, String type, String title, String message) {
        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .createdAt(LocalDateTime.now())
            .build();
        Notification saved = notificationRepository.save(notification);
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications", saved);
        return saved;
    }

    /**
     * Publica una notificación global para todos los clientes conectados.
     *
     * @param title título visible.
     * @param message contenido visible.
     */
    public void broadcast(String title, String message) {
        messagingTemplate.convertAndSend("/topic/plantio", title + " - " + message);
    }

    /**
     * Devuelve las notificaciones del usuario.
     *
     * @param user usuario autenticado.
     * @return notificaciones ordenadas.
     */
    @Transactional(readOnly = true)
    public List<Notification> findForUser(AppUser user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }
}
