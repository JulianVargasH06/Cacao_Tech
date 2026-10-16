package com.cacaotech.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/protegido")
public class ProtectedController {

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> pingProtegido(Authentication authentication) {
        String correo = authentication != null ? authentication.getName() : "anonimo";
        return ResponseEntity.ok(Map.of(
                "mensaje", "Acceso concedido a ruta protegida de Cacao Tech",
                "usuario", correo,
                "autoridades", authentication != null ? authentication.getAuthorities() : "ninguna"
        ));
    }
}
