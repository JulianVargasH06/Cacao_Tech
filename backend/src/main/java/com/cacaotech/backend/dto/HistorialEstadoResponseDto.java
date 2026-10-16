package com.cacaotech.backend.dto;

import java.time.LocalDateTime;

/** Un cambio de estado de un lote: de qué a qué, por qué, quién y cuándo. */
public record HistorialEstadoResponseDto(
        Long id,
        String estadoAnterior,
        String estadoNuevo,
        String motivo,
        String usuario,
        LocalDateTime fecha
) {}
