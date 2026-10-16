package com.cacaotech.backend.dto;

import java.time.LocalDateTime;

/** Respuesta de POST y GET /api/incidencias. */
public record IncidenciaResponseDto(
        Long id,
        Long transformacionId,
        String codigoTransformacion,
        Long loteId,
        String codigoLote,
        String tipo,
        String descripcion,
        LocalDateTime fecha,
        String usuario,
        Long alertaId // la alerta que se generó si se bloqueó el lote; null si no
) {}
