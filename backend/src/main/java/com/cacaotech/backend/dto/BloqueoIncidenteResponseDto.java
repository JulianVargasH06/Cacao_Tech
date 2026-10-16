package com.cacaotech.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado del bloqueo por incidente: qué se bloqueó, qué ya estaba bloqueado y cuánto se retiene. */
public record BloqueoIncidenteResponseDto(
        List<String> bloqueados,
        List<String> yaEstabanBloqueados,
        BigDecimal kgRetenidos,
        Long alertaId
) {}
