package com.marcoszamorano.practica2.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class NavigationAdvice {

    @ModelAttribute
    public void addNavigationAttributes(Model model, HttpServletRequest request, Principal principal) {
        model.addAttribute("authenticated", principal != null);
        model.addAttribute("currentUsername", principal != null ? principal.getName() : null);
        model.addAttribute("currentPath", request.getRequestURI());
    }
}