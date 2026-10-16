package com.cacaotech.backend.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de PATCH /api/usuarios/{id} (HU14): cambiar el rol y/o activar o desactivar la cuenta.
 * Los campos que lleguen en null no se modifican.
 */
public record UsuarioUpdateDto(
        @Pattern(regexp = "ADMINISTRADOR|OPERADOR", message = "El rol debe ser ADMINISTRADOR u OPERADOR")
        String rol,

        Boolean activo
) {}
