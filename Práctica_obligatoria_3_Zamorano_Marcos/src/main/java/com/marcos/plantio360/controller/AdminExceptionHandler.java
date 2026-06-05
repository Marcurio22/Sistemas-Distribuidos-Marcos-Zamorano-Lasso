/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.InvalidParameterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Maneja errores controlados de formularios administrativos.
 */
@Slf4j
@ControllerAdvice
public class AdminExceptionHandler {

    /**
     * Captura ficheros que superan el máximo configurado y vuelve al formulario de origen.
     *
     * @param exception excepción de tamaño máximo.
     * @param request petición original.
     * @return redirección al formulario/listado de origen.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUpload(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        log.warn("Multipart rechazado por tamaño. uri={}, contentLength={}, exception={}", request.getRequestURI(), request.getContentLengthLong(), exception.toString());
        RequestContextUtils.getOutputFlashMap(request).put("error", "La imagen supera el tamaño máximo permitido de 64MB. Selecciona una imagen más ligera.");
        return redirectBack(request);
    }

    /**
     * Captura errores generales de multipart sin atribuirlos siempre al tamaño del archivo.
     *
     * @param exception excepción multipart.
     * @param request petición original.
     * @return redirección al formulario/listado de origen.
     */
    @ExceptionHandler(MultipartException.class)
    public String handleMultipart(MultipartException exception, HttpServletRequest request) {
        log.warn("Error multipart. uri={}, contentLength={}, contentType={}, exception={}", request.getRequestURI(), request.getContentLengthLong(), request.getContentType(), exception.toString());
        RequestContextUtils.getOutputFlashMap(request).put("error", "No se pudo leer el formulario multipart. Si adjuntaste una imagen, comprueba que sea PNG, JPG, JPEG, WEBP o SVG y que no supere 64MB.");
        return redirectBack(request);
    }


    /**
     * Captura rechazos de Tomcat antes de entrar al controlador, como el límite
     * de número de partes multipart de Spring Boot 4 / Tomcat 11.
     */
    @ExceptionHandler(InvalidParameterException.class)
    public String handleInvalidParameter(InvalidParameterException exception, HttpServletRequest request) {
        log.warn(
            "Petición rechazada por límites del conector Tomcat. uri={}, contentLength={}, contentType={}, exception={}",
            request.getRequestURI(),
            request.getContentLengthLong(),
            request.getContentType(),
            exception.toString()
        );
        String message = exception.toString().contains("FileCountLimitExceededException")
            ? "El formulario tiene más campos multipart de los que Tomcat permite por defecto. La aplicación ya está configurada para permitir formularios administrativos; reconstruye el contenedor y vuelve a intentarlo."
            : "No se pudo procesar el formulario enviado. Revisa los campos y vuelve a intentarlo.";
        RequestContextUtils.getOutputFlashMap(request).put("error", message);
        return redirectBack(request);
    }

    /**
     * Devuelve al usuario a la URL previa cuando existe.
     *
     * @param request petición original.
     * @return redirección segura.
     */
    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains(request.getServerName())) {
            return "redirect:" + referer;
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/admin/players")) return "redirect:/admin/players/new";
        if (uri != null && uri.startsWith("/admin/products")) return "redirect:/admin/products/new";
        if (uri != null && uri.startsWith("/admin/matches")) return "redirect:/admin/matches/new";
        return "redirect:/admin";
    }
}
