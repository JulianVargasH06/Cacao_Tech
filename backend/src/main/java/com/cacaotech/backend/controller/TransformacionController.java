package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.CostoRequestDto;
import com.cacaotech.backend.dto.CostosTransformacionDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
import com.cacaotech.backend.dto.TransformacionResponseDto;
import com.cacaotech.backend.service.CostoService;
import com.cacaotech.backend.service.TransformacionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transformaciones")
public class TransformacionController {

    private final TransformacionService transformacionService;
    private final CostoService costoService;

    public TransformacionController(TransformacionService transformacionService, CostoService costoService) {
        this.transformacionService = transformacionService;
        this.costoService = costoService;
    }

    /** POST /api/transformaciones — registra una transformación (RF02). El responsable sale del token. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<TransformacionResponseDto> registrar(@Valid @RequestBody TransformacionRequestDto dto,
                                                               Authentication authentication) {
        TransformacionResponseDto respuesta = transformacionService.registrar(dto, authentication.getName());
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    /** POST /api/transformaciones/{id}/costos (HU05 · solo Administrador): agrega un costo y reparte el total. */
    @PostMapping("/{id}/costos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CostosTransformacionDto> registrarCosto(@PathVariable Long id,
                                                                  @Valid @RequestBody CostoRequestDto dto,
                                                                  Authentication authentication) {
        return new ResponseEntity<>(costoService.registrar(id, dto, authentication.getName()), HttpStatus.CREATED);
    }

    /** GET /api/transformaciones/{id}/costos (HU05 · solo Administrador) */
    @GetMapping("/{id}/costos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CostosTransformacionDto> costos(@PathVariable Long id) {
        return ResponseEntity.ok(costoService.consultar(id));
    }

    /** GET /api/transformaciones — historial de transformaciones, de la más reciente a la más antigua. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<TransformacionResponseDto>> listar() {
        return ResponseEntity.ok(transformacionService.listar());
    }
}
