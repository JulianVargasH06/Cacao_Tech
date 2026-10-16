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
        BigDecimal mermaPorcentaje,
        BigDecimal costoTotalCop, // suma de sus costos (HU05); 0 si aún no tiene
        AlertaGenerada alerta // null si la merma no superó el umbral
) {

    public record Entrada(Long loteId, String codigoLote, BigDecimal cantidadKg) {}

    /** loteId es el lote nuevo que se creó con lo que salió del proceso. */
    public record Salida(Long loteId, String codigo, BigDecimal cantidadKg,
                         BigDecimal costoAsignadoCop, BigDecimal costoUnitarioCop) {} // costos: null si aún no tiene

    /** Alerta de merma alta (HU04): si existe, los lotes resultantes quedaron BLOQUEADOS. */
    public record AlertaGenerada(Long id, String tipo, String descripcion) {}
}
