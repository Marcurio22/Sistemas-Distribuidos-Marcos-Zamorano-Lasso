/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.AssistantRequest;
import com.marcos.plantio360.dto.AssistantResponse;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.repository.FaqRepository;
import com.marcos.plantio360.service.AssistantService;
import com.marcos.plantio360.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador del asistente Blanquinegro Bot.
 */
@Controller
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistantService;
    private final CurrentUserService currentUserService;
    private final FaqRepository faqRepository;

    /** Muestra la pantalla del asistente. */
    @GetMapping("/assistant")
    public String assistant(Model model) {
        populateAssistantModel(model, new AssistantRequest(), null);
        return "assistant";
    }

    /** Procesa una pregunta desde Thymeleaf. */
    @PostMapping("/assistant")
    public String ask(@ModelAttribute AssistantRequest request, Model model) {
        AppUser user = currentUserService.require();
        populateAssistantModel(model, request, assistantService.answer(user, request.getQuestion()));
        return "assistant";
    }

    /**
     * Carga los datos comunes del asistente para Thymeleaf.
     *
     * @param model modelo de vista.
     * @param request pregunta actual.
     * @param response respuesta generada o nula.
     */
    private void populateAssistantModel(Model model, AssistantRequest request, AssistantResponse response) {
        model.addAttribute("assistantRequest", request);
        model.addAttribute("faqs", faqRepository.findByActiveTrue());
        if (response != null) {
            model.addAttribute("response", response);
        }
    }

    /** Procesa una pregunta vía API REST. */
    @PostMapping("/api/assistant")
    @ResponseBody
    public AssistantResponse apiAsk(@RequestBody AssistantRequest request) {
        return assistantService.answer(currentUserService.require(), request.getQuestion());
    }
}
