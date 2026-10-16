package com.cacaotech.backend.dto;

import java.time.LocalDateTime;

/** Respuesta de GET /api/alertas (HU04). */
public record AlertaResponseDto(
        Long id,
        String tipo, // MERMA_ALTA o INCIDENCIA
        String severidad,
        String descripcion,
        Long loteId,
        String codigoLote,
        Long transformacionId,
        String codigoTransformacion,
        String estado, // ACTIVA o RESUELTA
        LocalDateTime creadaEn,
        LocalDateTime resueltaEn,
        String resueltaPor
) {}
