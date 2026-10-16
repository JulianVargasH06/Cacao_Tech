package com.cacaotech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/incidencias: un hallazgo durante una transformación.
 * Si bloquearLote es true, el lote indicado queda BLOQUEADO y se genera una alerta.
 */
public record IncidenciaRequestDto(
        @NotNull(message = "La transformación es obligatoria")
        Long transformacionId,

        Long loteId,

        @NotBlank(message = "El tipo de incidencia es obligatorio")
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        String tipo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion,

        boolean bloquearLote
) {}
