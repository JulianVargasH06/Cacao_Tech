package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.FichaPublicaDto;
import com.cacaotech.backend.service.TrazabilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública para el comprador (HU06). No requiere token: SecurityConfig deja libre /api/publico/**.
 */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final TrazabilidadService trazabilidadService;

    public PublicoController(TrazabilidadService trazabilidadService) {
        this.trazabilidadService = trazabilidadService;
    }

    /**
     * Ficha pública de un producto: origen, recorrido y estado.
     * Responde en las dos rutas acordadas por el equipo: la de la tabla de endpoints (/productos)
     * y la del plan del backend (/trazabilidad).
     */
    @GetMapping({"/productos/{codigo}", "/trazabilidad/{codigo}"})
    public ResponseEntity<FichaPublicaDto> fichaPublica(@PathVariable String codigo) {
        return ResponseEntity.ok(trazabilidadService.fichaPublica(codigo));
    }
}
