package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    /** GET /api/lotes — inventario (HU08) y base del historial. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<Lote>> listar() {
        return ResponseEntity.ok(loteService.listar());
    }

    @PostMapping("/recepcion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<Lote> registrarRecepcion(@Valid @RequestBody LoteRequestDto dto) {
        Lote nuevoLote = loteService.registrarRecepcion(dto);
        return new ResponseEntity<>(nuevoLote, HttpStatus.CREATED);
    }
}