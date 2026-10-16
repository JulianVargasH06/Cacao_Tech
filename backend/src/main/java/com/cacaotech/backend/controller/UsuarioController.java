package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.UsuarioRequestDto;
import com.cacaotech.backend.dto.UsuarioResponseDto;
import com.cacaotech.backend.dto.UsuarioUpdateDto;
import com.cacaotech.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de cuentas internas (HU14). Todo el controlador es solo para el Administrador. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDto>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDto> crear(@Valid @RequestBody UsuarioRequestDto dto) {
        return new ResponseEntity<>(usuarioService.crear(dto), HttpStatus.CREATED);
    }

    /** Cambia el rol y/o activa o desactiva la cuenta. Un usuario desactivado no puede iniciar sesión. */
    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponseDto> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody UsuarioUpdateDto dto,
                                                         Authentication authentication) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto, authentication.getName()));
    }
}
