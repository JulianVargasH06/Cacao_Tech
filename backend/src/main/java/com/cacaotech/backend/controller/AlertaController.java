package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.AlertaResponseDto;
import com.cacaotech.backend.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    /** GET /api/alertas?estado=ACTIVA|RESUELTA (HU04 · solo Administrador). Sin estado devuelve todas. */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AlertaResponseDto>> listar(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(alertaService.listar(estado));
    }

    /** PATCH /api/alertas/{id}/resolver (solo Administrador): marca la alerta como revisada. */
    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AlertaResponseDto> resolver(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(alertaService.resolver(id, authentication.getName()));
    }
}
