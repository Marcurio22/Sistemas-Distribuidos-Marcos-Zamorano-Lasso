/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.ChatPayload;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.model.ChatMessage;
import com.marcos.plantio360.repository.ChatMessageRepository;
import com.marcos.plantio360.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Controlador WebSocket del Muro Blanquinegro.
 */
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatMessageRepository chatMessageRepository;
    private final CurrentUserService currentUserService;

    /** Muestra el chat de aficionados. */
    @GetMapping("/chat")
    public String chat(Model model) {
        List<ChatMessage> messages = chatMessageRepository.findTop50ByOrderByCreatedAtDesc();
        Collections.reverse(messages);
        model.addAttribute("messages", messages);
        return "chat";
    }

    /** Recibe mensaje STOMP y lo retransmite al canal público. */
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage send(ChatPayload payload) {
        AppUser user = currentUserService.current().orElse(null);
        String displayName = user == null ? "Aficionado" : user.getFullName();
        ChatMessage message = ChatMessage.builder()
            .user(user)
            .displayName(displayName)
            .content(payload.getContent())
            .createdAt(LocalDateTime.now())
            .build();
        return chatMessageRepository.save(message);
    }
}
