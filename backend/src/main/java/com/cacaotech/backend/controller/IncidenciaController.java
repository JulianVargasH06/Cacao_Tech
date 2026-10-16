package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.IncidenciaRequestDto;
import com.cacaotech.backend.dto.IncidenciaResponseDto;
import com.cacaotech.backend.service.IncidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {

    private final IncidenciaService incidenciaService;

    public IncidenciaController(IncidenciaService incidenciaService) {
        this.incidenciaService = incidenciaService;
    }

    /** POST /api/incidencias (RF03): registra un hallazgo; con bloquearLote=true bloquea el lote y genera alerta. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<IncidenciaResponseDto> registrar(@Valid @RequestBody IncidenciaRequestDto dto,
                                                           Authentication authentication) {
        return new ResponseEntity<>(incidenciaService.registrar(dto, authentication.getName()), HttpStatus.CREATED);
    }

    /** GET /api/incidencias?transformacionId=&loteId= (ambos opcionales) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<IncidenciaResponseDto>> listar(@RequestParam(required = false) Long transformacionId,
                                                              @RequestParam(required = false) Long loteId) {
        return ResponseEntity.ok(incidenciaService.listar(transformacionId, loteId));
    }
}
