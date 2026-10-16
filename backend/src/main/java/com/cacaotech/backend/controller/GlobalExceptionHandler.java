package com.cacaotech.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errores.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Error de validación de datos");
        response.put("detalles", errores);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /** JSON mal formado o con tipos inválidos (ej. una fecha que no es yyyy-MM-ddTHH:mm:ss). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMensajeIlegible(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "El cuerpo de la petición no es válido. Revisa el formato de los datos");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /** El usuario tiene sesión pero su rol no le permite esta operación (@PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "No tienes permiso para realizar esta operación");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /** Dos operaciones modificaron el mismo lote a la vez (@Version en Lote): la segunda debe reintentar. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrencia(ObjectOptimisticLockingFailureException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "El lote fue modificado por otra operación al mismo tiempo. Intenta de nuevo");
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
}
