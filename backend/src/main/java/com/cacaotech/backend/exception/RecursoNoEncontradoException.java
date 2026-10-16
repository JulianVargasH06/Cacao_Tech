package com.cacaotech.backend.exception;

/** Se traduce a 404 en GlobalExceptionHandler. Útil en endpoints públicos: "ese código no existe". */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
