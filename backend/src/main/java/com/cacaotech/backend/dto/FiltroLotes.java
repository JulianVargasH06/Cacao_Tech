package com.cacaotech.backend.dto;

import com.cacaotech.backend.model.Lote;

import java.time.LocalDate;

/**
 * Filtros opcionales de GET /api/lotes y GET /api/lotes/exportar (HU11).
 * Los que lleguen en null no se aplican.
 */
public record FiltroLotes(
        String codigo, // contiene, sin importar mayúsculas
        String origen, // productor u origen contiene, sin importar mayúsculas (ej. "Rivera")
        Lote.EstadoLote estado,
        Lote.TipoLote tipoLote,
        LocalDate desde, // fechaRegistro desde este día, inclusive
        LocalDate hasta // fechaRegistro hasta este día, inclusive
) {
    public static FiltroLotes ninguno() {
        return new FiltroLotes(null, null, null, null, null, null);
    }
}
