package com.cacaotech.backend.dto;

import java.math.BigDecimal;

/** Respuesta de GET /api/lotes/{id}/margen (HU05): margen sobre venta de un lote resultante. */
public record MargenDto(
        Long loteId,
        String codigo,
        BigDecimal costoUnitarioCop, // costo por kg asignado desde su transformación
        BigDecimal precioVentaKgCop,
        BigDecimal gananciaPorKgCop, // precio - costo (negativa si se vende por debajo del costo)
        BigDecimal margenPorcentaje // (precio - costo) / precio × 100 (MargenCalculadora)
) {}
