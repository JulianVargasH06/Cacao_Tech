package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.BloqueoIncidenteRequestDto;
import com.cacaotech.backend.dto.BloqueoIncidenteResponseDto;
import com.cacaotech.backend.dto.CambioEstadoRequestDto;
import com.cacaotech.backend.dto.FiltroLotes;
import com.cacaotech.backend.dto.HistorialEstadoResponseDto;
import com.cacaotech.backend.dto.ImpactoLoteDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.MargenDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.service.CostoService;
import com.cacaotech.backend.service.EstadoLoteService;
import com.cacaotech.backend.service.ImpactoLoteService;
import com.cacaotech.backend.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;
    private final EstadoLoteService estadoLoteService;
    private final CostoService costoService;
    private final ImpactoLoteService impactoLoteService;

    public LoteController(LoteService loteService, EstadoLoteService estadoLoteService, CostoService costoService,
                          ImpactoLoteService impactoLoteService) {
        this.loteService = loteService;
        this.estadoLoteService = estadoLoteService;
        this.costoService = costoService;
        this.impactoLoteService = impactoLoteService;
    }

    /** GET /api/lotes — inventario (HU08) y base del historial. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERADOR')")
    public ResponseEntity<List<Lote>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Lote.EstadoLote estado,
            @RequestParam(required = false) Lote.TipoLote tipoLote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(loteService.listar(new FiltroLotes(codigo, estado, tipoLote, desde, hasta)));
    }

    /** GET /api/lotes/exportar (HU11 · solo Administrador): CSV con los mismos filtros de GET /api/lotes. */
    @GetMapping("/exportar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) Lote.EstadoLote estado,
            @RequestParam(required = false) Lote.TipoLote tipoLote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        byte[] csv = loteService.exportarCsv(new FiltroLotes(codigo, estado, tipoLote, desde, hasta));
        String archivo = "lotes-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
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

    /** GET /api/lotes/{id}/margen?precioVentaKg=18000 (HU05 · solo Administrador): margen sobre venta. */
    @GetMapping("/{id}/margen")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MargenDto> margen(@PathVariable Long id, @RequestParam BigDecimal precioVentaKg) {
        return ResponseEntity.ok(costoService.margen(id, precioVentaKg));
    }

    /** GET /api/lotes/{id}/impacto (solo Administrador): origen, afectados y relacionados, con cantidades. */
    @GetMapping("/{id}/impacto")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ImpactoLoteDto> impacto(@PathVariable Long id) {
        return ResponseEntity.ok(impactoLoteService.impacto(id));
    }

    /** POST /api/lotes/{id}/bloqueo-incidente (solo Administrador): bloquea el lote reportado y sus derivados. */
    @PostMapping("/{id}/bloqueo-incidente")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<BloqueoIncidenteResponseDto> bloquearPorIncidente(@PathVariable Long id,
                                                                            @Valid @RequestBody BloqueoIncidenteRequestDto dto,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(impactoLoteService.bloquearPorIncidente(id, dto, authentication.getName()));
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