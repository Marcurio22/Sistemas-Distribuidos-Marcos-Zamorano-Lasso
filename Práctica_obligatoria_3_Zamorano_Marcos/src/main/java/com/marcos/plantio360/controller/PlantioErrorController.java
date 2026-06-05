/**
 * El Plantío 360 - Controlador de errores HTTP.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * Sustituye el fallback genérico de /error por mensajes útiles en administración.
 */
@Slf4j
@Controller
public class PlantioErrorController implements ErrorController {

    /** Gestiona errores que no llegan a los controladores MVC normales. */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        Integer status = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String originalPath = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        log.warn("Error dispatch controlado. status={}, originalPath={}, method={}, contentLength={}, contentType={}, referer={}, exception={}",
            status,
            originalPath,
            request.getMethod(),
            request.getContentLengthLong(),
            request.getContentType(),
            request.getHeader("Referer"),
            exception == null ? null : exception.toString());

        if (Integer.valueOf(413).equals(status) && originalPath != null && originalPath.startsWith("/admin/")) {
            redirectAttributes.addFlashAttribute(
                "error",
                "El formulario multipart fue rechazado antes de llegar al controlador. " +
                    "Si no adjuntaste una imagen grande, revisa server.tomcat.max-part-count: " +
                    "los formularios de administración tienen más de 10 campos y Spring Boot 4/Tomcat 11 puede devolver 413 si ese límite no está configurado."
            );
            return redirectBackToAdminForm(request, originalPath);
        }

        model.addAttribute("status", status);
        model.addAttribute("path", originalPath);
        model.addAttribute("message", messageForStatus(status));
        return "error";
    }

    private String redirectBackToAdminForm(HttpServletRequest request, String originalPath) {
        String referer = request.getHeader("Referer");
        if (isSafeSameHostReferer(referer, request)) {
            return "redirect:" + referer;
        }
        if (originalPath.startsWith("/admin/players")) return "redirect:/admin/players/new";
        if (originalPath.startsWith("/admin/matches")) return "redirect:/admin/matches/new";
        if (originalPath.startsWith("/admin/products")) return "redirect:/admin/products/new";
        if (originalPath.startsWith("/admin/users")) return "redirect:/admin/users/new";
        return "redirect:/admin";
    }

    private boolean isSafeSameHostReferer(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) return false;
        try {
            URI uri = URI.create(referer);
            return request.getServerName().equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String messageForStatus(Integer status) {
        if (status == null) return "La aplicación ha capturado un problema inesperado.";
        return switch (status) {
            case 400 -> "La petición enviada no es válida.";
            case 401 -> "Debes iniciar sesión para acceder a este recurso.";
            case 403 -> "No tienes permisos para acceder a este recurso.";
            case 404 -> "No se ha encontrado la página solicitada.";
            case 413 -> "El formulario multipart fue rechazado por límites de Tomcat.";
            default -> "La aplicación ha capturado un problema inesperado.";
        };
    }
}
