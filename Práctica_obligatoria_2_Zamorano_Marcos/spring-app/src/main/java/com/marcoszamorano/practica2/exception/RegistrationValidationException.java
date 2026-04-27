package com.marcoszamorano.practica2.exception;

import java.util.Map;

public class RegistrationValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public RegistrationValidationException(Map<String, String> fieldErrors) {
        super("Formulario de registro no válido");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}