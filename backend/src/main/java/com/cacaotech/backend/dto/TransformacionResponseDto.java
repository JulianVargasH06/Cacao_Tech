package com.cacaotech.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de POST y GET /api/transformaciones.
 * Se devuelve un DTO (y no la entidad) para no exponer al Usuario completo ni las relaciones LAZY.
 */
public record TransformacionResponseDto(
        Long id,
        String codigo,
        String tipo,
        LocalDateTime fecha,
        String responsable,
        String observaciones,
        List<Entrada> entradas,
        List<Salida> salidas,
        BigDecimal mermaKg,
        BigDecimal mermaPorcentaje
) {

    public record Entrada(Long loteId, String codigoLote, BigDecimal cantidadKg) {}

    /** loteId es el lote nuevo que se creó con lo que salió del proceso. */
    public record Salida(Long loteId, String codigo, BigDecimal cantidadKg) {}
}
