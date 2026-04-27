package com.marcoszamorano.practica2.controller;

import com.marcoszamorano.practica2.dto.RegisterForm;
import com.marcoszamorano.practica2.exception.RegistrationValidationException;
import com.marcoszamorano.practica2.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/lab";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/lab";
        }

        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }

        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registerForm") RegisterForm registerForm,
                           Authentication authentication,
                           Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/lab";
        }

        try {
            userService.registerNewUser(registerForm);
            return "redirect:/login?registered=true";

        } catch (RegistrationValidationException ex) {
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            model.addAttribute("globalError", "Revisa los campos marcados y corrige el formulario.");
            return "register";

        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("globalError",
                    "No se ha podido crear la cuenta porque el usuario o el correo ya existen, o la base de datos ha rechazado la operación.");
            return "register";

        } catch (Exception ex) {
            model.addAttribute("globalError",
                    "No se ha podido completar el registro en este momento. Inténtalo de nuevo más tarde.");
            return "register";
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}