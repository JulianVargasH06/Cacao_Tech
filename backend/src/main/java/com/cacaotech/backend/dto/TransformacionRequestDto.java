package com.cacaotech.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de POST /api/transformaciones (RF02).
 * El responsable no viaja en el cuerpo: se toma del usuario autenticado (token JWT).
 */
public record TransformacionRequestDto(
        @NotBlank(message = "El tipo de transformación es obligatorio")
        @Size(max = 50, message = "El tipo no puede superar 50 caracteres")
        String tipo,

        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDateTime fecha,

        @NotEmpty(message = "Debe haber al menos un lote de entrada")
        List<@Valid Entrada> entradas,

        @NotEmpty(message = "Debe haber al menos una salida")
        List<@Valid Salida> salidas,

        @Size(max = 2000, message = "Las observaciones no pueden superar 2000 caracteres")
        String observaciones
) {

    /** Cacao que se toma de un lote existente. */
    public record Entrada(
            @NotNull(message = "El lote de entrada es obligatorio")
            Long loteId,

            @NotNull(message = "La cantidad de entrada es obligatoria")
            @Positive(message = "La cantidad de entrada debe ser mayor a cero")
            BigDecimal cantidadKg
    ) {}

    /** Producto obtenido: se registra como un lote nuevo con este código. */
    public record Salida(
            @NotBlank(message = "El código del lote de salida es obligatorio")
            @Size(max = 30, message = "El código no puede superar 30 caracteres")
            String codigo,

            @NotNull(message = "La cantidad de salida es obligatoria")
            @Positive(message = "La cantidad de salida debe ser mayor a cero")
            BigDecimal cantidadKg
    ) {}
}
