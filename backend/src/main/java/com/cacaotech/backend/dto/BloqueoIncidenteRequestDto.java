package com.cacaotech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/lotes/{id}/bloqueo-incidente: bloquea el lote reportado y sus derivados.
 * Con incluirRelacionados = true también bloquea lo que comparte su mismo origen.
 */
public record BloqueoIncidenteRequestDto(
        @NotBlank(message = "El motivo es obligatorio (ej. qué reportó el comprador)")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo,

        boolean incluirRelacionados
) {}
