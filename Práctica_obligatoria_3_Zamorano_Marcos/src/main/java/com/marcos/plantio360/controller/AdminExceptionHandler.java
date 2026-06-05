/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Maneja errores controlados de formularios administrativos.
 */
@ControllerAdvice
public class AdminExceptionHandler {

    /**
     * Captura errores de multipart para evitar páginas 413 sin controlar cuando se suben imágenes demasiado grandes.
     *
     * @param exception excepción de subida.
     * @param request petición original.
     * @return redirección segura al formulario que originó el fallo.
     */
    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public String handleMultipart(Exception exception, HttpServletRequest request) {
        String message = exception instanceof MaxUploadSizeExceededException
            ? "La imagen supera el tamaño máximo permitido de 64MB. Selecciona otra imagen; el archivo rechazado no se ha guardado."
            : "No se pudo leer el archivo adjunto del formulario. Comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.";
        RequestContextUtils.getOutputFlashMap(request).put("error", message);
        return "redirect:" + safeRefererPath(request);
    }

    /**
     * Devuelve una ruta interna segura basada en Referer para mantener al usuario en el formulario.
     *
     * @param request petición recibida.
     * @return ruta interna de retorno.
     */
    private String safeRefererPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) return "/admin";
        try {
            URI uri = URI.create(referer);
            String path = uri.getPath();
            if (path != null && path.startsWith("/admin")) return path;
        } catch (IllegalArgumentException ignored) {
            return "/admin";
        }
        return "/admin";
    }
}
