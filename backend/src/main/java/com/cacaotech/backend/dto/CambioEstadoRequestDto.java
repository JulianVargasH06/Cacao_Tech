package com.cacaotech.backend.dto;

import com.cacaotech.backend.model.Lote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de PATCH /api/lotes/{id}/estado (HU09): el motivo es obligatorio. */
public record CambioEstadoRequestDto(
        @NotNull(message = "El estado es obligatorio (LIBERADO o BLOQUEADO)")
        Lote.EstadoLote estado,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
        String motivo
) {}
