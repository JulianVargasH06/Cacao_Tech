package com.cacaotech.backend.dto;

import java.time.LocalDateTime;

/** Un usuario interno. Nunca incluye la contraseña (ni su hash). */
public record UsuarioResponseDto(
        Long id,
        String nombre,
        String correo,
        String rol,
        boolean activo,
        LocalDateTime creadoEn
) {}
