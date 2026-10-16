package com.cacaotech.backend.dto;

import java.time.LocalDateTime;

/**
 * Un registro de GET /api/auditoria (HU10).
 * valoresAnteriores y valoresNuevos son JSON en texto, tal como se guardan en la tabla.
 */
public record AuditoriaResponseDto(
        Long id,
        String usuario, // nombre de quien hizo el cambio ("sistema" si no hubo usuario autenticado)
        String correoUsuario,
        String entidad,
        Long entidadId,
        String accion,
        String valoresAnteriores,
        String valoresNuevos,
        LocalDateTime fecha
) {}
