package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.AuditoriaResponseDto;
import com.cacaotech.backend.service.AuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /**
     * GET /api/auditoria (HU10 · solo Administrador). Todos los filtros son opcionales, por ejemplo:
     * /api/auditoria?entidad=Lote&entidadId=3, ?accion=CONSUMO, ?usuario=operador@cacaotech.com,
     * ?desde=2026-10-01&hasta=2026-10-16
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AuditoriaResponseDto>> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(auditoriaService.listar(entidad, entidadId, accion, usuario, desde, hasta));
    }
}
