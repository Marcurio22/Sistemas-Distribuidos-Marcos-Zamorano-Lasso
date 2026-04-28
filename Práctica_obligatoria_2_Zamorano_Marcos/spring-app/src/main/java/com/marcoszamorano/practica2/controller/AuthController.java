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

    /**
     * Controlador responsable de la parte pública de autenticación:
     * - login
     * - registro
     *
     * No gestiona la autenticación en sí, ya que eso lo realiza Spring Security,
     * pero sí controla las vistas y el tratamiento de errores del registro.
     */
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Si el usuario ya está autenticado, no tiene sentido volver a mostrar
     * la pantalla de login, por lo que se redirige al dashboard.
     */
    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    /**
     * Muestra el formulario de registro.
     * Si el usuario ya ha iniciado sesión, se evita el registro redundante
     * redirigiéndolo también al dashboard.
     */
    @GetMapping("/register")
    public String registerForm(Authentication authentication, Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }

        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }

        return "register";
    }

    /**
     * Procesa el formulario de registro.
     *
     * Casos contemplados:
     * - validación funcional de campos
     * - errores de integridad de base de datos
     * - errores inesperados
     *
     * En todos los casos se intenta volver a la misma vista con un mensaje
     * adecuado, evitando que el usuario vea un error genérico.
     */
    @PostMapping("/register")
    public String register(@ModelAttribute("registerForm") RegisterForm registerForm,
                           Authentication authentication,
                           Model model) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
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

    /**
     * Utilidad para comprobar si la autenticación actual corresponde
     * a un usuario real y no a un token anónimo de Spring Security.
     */
    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}