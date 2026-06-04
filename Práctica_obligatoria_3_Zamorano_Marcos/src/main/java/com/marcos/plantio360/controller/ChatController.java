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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("chatDisplayName", currentUserService.current()
            .map(AppUser::getFullName)
            .filter(name -> !name.isBlank())
            .orElse("Aficionado Blanquinegro"));
        return "chat";
    }


    /**
     * Elimina un mensaje del muro desde administración.
     *
     * @param id identificador del mensaje.
     * @param redirectAttributes atributos flash.
     * @return redirección al muro.
     */
    @PostMapping("/chat/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        chatMessageRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Mensaje eliminado del Muro Blanquinegro.");
        return "redirect:/chat";
    }

    /** Recibe mensaje STOMP y lo retransmite al canal público. */
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public ChatMessage send(ChatPayload payload) {
        AppUser user = currentUserService.current().orElse(null);
        String displayName = user == null ? sanitizeDisplayName(payload.getDisplayName()) : user.getFullName();
        ChatMessage message = ChatMessage.builder()
            .user(user)
            .displayName(displayName)
            .content(payload.getContent())
            .createdAt(LocalDateTime.now())
            .build();
        return chatMessageRepository.save(message);
    }

    /**
     * Limpia el nombre visible recibido desde la página autenticada cuando el contexto STOMP no trae usuario.
     *
     * @param displayName nombre visible propuesto por el cliente.
     * @return nombre seguro para mostrar en el muro.
     */
    private String sanitizeDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "Aficionado Blanquinegro";
        }
        String cleaned = displayName.replaceAll("[<>]", "").trim();
        return cleaned.length() > 60 ? cleaned.substring(0, 60) : cleaned;
    }
}
