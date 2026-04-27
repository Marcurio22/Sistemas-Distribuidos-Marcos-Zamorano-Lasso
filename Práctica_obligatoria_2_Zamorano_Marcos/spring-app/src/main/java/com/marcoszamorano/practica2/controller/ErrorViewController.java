package com.marcoszamorano.practica2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorViewController {

    @GetMapping("/forbidden")
    public String forbidden() {
        return "error/403";
    }
}