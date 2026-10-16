package com.cacaotech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Cuerpo de POST /api/transformaciones/{id}/costos (HU05): un costo del proceso, en pesos colombianos. */
public record CostoRequestDto(
        @NotBlank(message = "La categoría es obligatoria (ej. MANO_DE_OBRA, ENERGIA, EMPAQUE)")
        @Size(max = 50, message = "La categoría no puede superar 50 caracteres")
        String categoria,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El valor es obligatorio")
        @Positive(message = "El valor debe ser mayor a cero")
        BigDecimal valorCop
) {}
