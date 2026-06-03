/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import com.marcos.plantio360.dto.JwtResponse;
import com.marcos.plantio360.dto.LoginRequest;
import com.marcos.plantio360.dto.RegisterRequest;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.service.JwtService;
import com.marcos.plantio360.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Controlador de autenticación MVC y REST con JWT en cookie HttpOnly.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    /**
     * Muestra formulario de login.
     *
     * @param model modelo de vista.
     * @return plantilla de login.
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    /**
     * Procesa login MVC y emite JWT en cookie HttpOnly.
     *
     * @param request credenciales.
     * @param response respuesta HTTP.
     * @param model modelo de error.
     * @return redirección a dashboard.
     */
    @PostMapping("/login")
    public String login(@ModelAttribute LoginRequest request, HttpServletResponse response, Model model) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            AppUser user = userService.findByEmail(request.getEmail());
            addJwtCookie(response, jwtService.generateToken(user.getEmail(), user.getRole()));
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", "Credenciales incorrectas");
            return "auth/login";
        }
    }

    /**
     * Muestra formulario de registro.
     *
     * @param model modelo de vista.
     * @return plantilla de registro.
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    /**
     * Registra usuario MVC y emite JWT.
     *
     * @param request datos de usuario.
     * @param response respuesta HTTP.
     * @param model modelo de error.
     * @return dashboard si se completa.
     */
    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest request, HttpServletResponse response, Model model) {
        try {
            AppUser user = userService.register(request);
            addJwtCookie(response, jwtService.generateToken(user.getEmail(), user.getRole()));
            return "redirect:/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    /**
     * Elimina la cookie JWT para cerrar sesión.
     *
     * @param response respuesta HTTP.
     * @return redirección a home.
     */
    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("PLANTIO360_JWT", "")
            .httpOnly(true).sameSite("Lax").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }

    /**
     * Login REST para obtener token Bearer.
     *
     * @param request credenciales.
     * @return token JWT.
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<JwtResponse> apiLogin(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        AppUser user = userService.findByEmail(request.getEmail());
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(new JwtResponse(token, user.getEmail(), user.getRole()));
    }

    /**
     * Registro REST para obtener token Bearer.
     *
     * @param request datos de usuario.
     * @return token JWT.
     */
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<JwtResponse> apiRegister(@RequestBody RegisterRequest request) {
        AppUser user = userService.register(request);
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(new JwtResponse(token, user.getEmail(), user.getRole()));
    }

    /**
     * Añade cookie segura de sesión JWT.
     *
     * @param response respuesta HTTP.
     * @param token JWT generado.
     */
    private void addJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("PLANTIO360_JWT", token)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofHours(24))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
