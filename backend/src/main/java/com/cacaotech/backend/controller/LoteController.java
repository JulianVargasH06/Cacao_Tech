package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.CambioEstadoRequestDto;
import com.cacaotech.backend.dto.HistorialEstadoResponseDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.service.EstadoLoteService;
import com.cacaotech.backend.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;
    private final EstadoLoteService estadoLoteService;

    public LoteController(LoteService loteService, EstadoLoteService estadoLoteService) {
        this.loteService = loteService;
        this.estadoLoteService = estadoLoteService;
    }

    /** GET /api/lotes — inventario (HU08) y base del historial. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<Lote>> listar() {
        return ResponseEntity.ok(loteService.listar());
    }

    /** PATCH /api/lotes/{id}/estado (HU09 · solo Administrador): liberar o bloquear con motivo obligatorio. */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Lote> cambiarEstado(@PathVariable Long id,
                                              @Valid @RequestBody CambioEstadoRequestDto dto,
                                              Authentication authentication) {
        return ResponseEntity.ok(
                estadoLoteService.cambiarEstadoManual(id, dto.estado(), dto.motivo(), authentication.getName()));
    }

    /** GET /api/lotes/{id}/historial-estados: cada cambio de estado con su motivo, del más reciente al más antiguo. */
    @GetMapping("/{id}/historial-estados")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<HistorialEstadoResponseDto>> historialEstados(@PathVariable Long id) {
        return ResponseEntity.ok(estadoLoteService.historial(id));
    }

    @PostMapping("/recepcion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Lote> registrarRecepcion(@Valid @RequestBody LoteRequestDto dto) {
        Lote nuevoLote = loteService.registrarRecepcion(dto);
        return new ResponseEntity<>(nuevoLote, HttpStatus.CREATED);
    }
}