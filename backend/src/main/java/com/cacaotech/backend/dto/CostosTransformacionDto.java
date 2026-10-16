package com.cacaotech.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Costos de una transformación (HU05) y cómo se reparten entre sus lotes resultantes.
 * El reparto es proporcional a los kg de cada salida (CostoDistribucionService).
 */
public record CostosTransformacionDto(
        Long transformacionId,
        String codigo,
        BigDecimal costoTotalCop,
        List<Item> costos,
        List<SalidaCosto> salidas
) {

    public record Item(Long id, String categoria, String descripcion, BigDecimal valorCop,
                       LocalDateTime fecha, String usuario) {}

    /** costoUnitarioCop = costoAsignadoCop / cantidadKg */
    public record SalidaCosto(Long loteId, String codigo, BigDecimal cantidadKg,
                              BigDecimal costoAsignadoCop, BigDecimal costoUnitarioCop) {}
}
