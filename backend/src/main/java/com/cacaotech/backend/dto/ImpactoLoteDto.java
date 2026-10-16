package com.cacaotech.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Análisis de impacto de un lote reportado (incidente / retiro de producto):
 * de dónde viene, qué está afectado y qué comparte su mismo origen. Las cantidades
 * "disponibles" son lo que todavía está en bodega y habría que retener.
 */
public record ImpactoLoteDto(
        LoteResumen lote, // el lote o producto reportado
        List<LoteResumen> origenes, // lotes de finca de donde salió
        List<LoteResumen> afectados, // el lote reportado + todo lo que se produjo a partir de él
        List<LoteResumen> relacionados, // otros lotes que salieron del mismo origen (posible riesgo)
        List<String> transformaciones, // códigos de las transformaciones involucradas
        BigDecimal kgAfectadosDisponibles,
        BigDecimal kgRelacionadosDisponibles
) {

    public record LoteResumen(
            Long id,
            String codigo,
            String tipoLote,
            String estado,
            String productor,
            String origen,
            BigDecimal cantidadInicial,
            BigDecimal cantidadDisponible
    ) {}
}
