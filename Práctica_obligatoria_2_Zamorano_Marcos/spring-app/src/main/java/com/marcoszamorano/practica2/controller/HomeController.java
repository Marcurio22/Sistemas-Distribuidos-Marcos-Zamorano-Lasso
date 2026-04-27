package com.marcoszamorano.practica2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "Práctica obligatoria 2");
        model.addAttribute("subtitle", "Sistema distribuido con Spring Boot, Flask y MySQL");
        return "home";
    }
}