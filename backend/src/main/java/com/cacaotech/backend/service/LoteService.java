package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.FiltroLotes;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.repository.LoteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoteService {

    private final LoteRepository loteRepository;
    private final AuditoriaService auditoriaService;

    public LoteService(LoteRepository loteRepository, AuditoriaService auditoriaService) {
        this.loteRepository = loteRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Lote registrarRecepcion(LoteRequestDto dto) {
        // Validar que el código del lote sea único
        if (loteRepository.findByCodigo(dto.getCodigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un lote registrado con el código: " + dto.getCodigo());
        }

        Lote lote = new Lote();
        lote.setCodigo(dto.getCodigo());
        lote.setProductor(dto.getProductor());
        lote.setOrigen(dto.getOrigen());
        lote.setFechaRegistro(dto.getFechaRegistro());
        lote.setCantidadInicial(dto.getCantidadInicial());
        
        // Al recibir el lote, todo lo recibido está disponible para futuras transformaciones (RF08)
        lote.setCantidadDisponible(dto.getCantidadInicial());
        
        lote.setUnidad(dto.getUnidad() != null ? dto.getUnidad() : "KG");
        lote.setTipoLote(Lote.TipoLote.RECEPCION);
        lote.setEstado(Lote.EstadoLote.PENDIENTE);

        Lote guardado = loteRepository.save(lote);
        auditoriaService.registrar("Lote", guardado.getId(), "CREAR", null, valoresDeLote(guardado));
        return guardado;
    }

    /** Lo que se guarda en la auditoría al crear un lote. LinkedHashMap porque productor y origen pueden ser null. */
    public static Map<String, Object> valoresDeLote(Lote lote) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("codigo", lote.getCodigo());
        valores.put("productor", lote.getProductor());
        valores.put("origen", lote.getOrigen());
        valores.put("fechaRegistro", lote.getFechaRegistro());
        valores.put("cantidadInicial", AuditoriaService.cantidad(lote.getCantidadInicial()));
        valores.put("unidad", lote.getUnidad());
        valores.put("tipoLote", lote.getTipoLote());
        valores.put("estado", lote.getEstado());
        return valores;
    }

    /** Lotes del más reciente al más antiguo; los filtros son opcionales (HU11). */
    @Transactional(readOnly = true)
    public List<Lote> listar(FiltroLotes filtro) {
        Specification<Lote> condiciones = (raiz, consulta, cb) -> {
            List<Predicate> lista = new ArrayList<>();
            if (filtro.codigo() != null && !filtro.codigo().isBlank()) {
                lista.add(cb.like(cb.lower(raiz.get("codigo")), "%" + filtro.codigo().trim().toLowerCase() + "%"));
            }
            if (filtro.origen() != null && !filtro.origen().isBlank()) {
                String patron = "%" + filtro.origen().trim().toLowerCase() + "%";
                lista.add(cb.or(cb.like(cb.lower(raiz.get("productor")), patron),
                        cb.like(cb.lower(raiz.get("origen")), patron)));
            }
            if (filtro.estado() != null) lista.add(cb.equal(raiz.get("estado"), filtro.estado()));
            if (filtro.tipoLote() != null) lista.add(cb.equal(raiz.get("tipoLote"), filtro.tipoLote()));
            if (filtro.desde() != null) lista.add(cb.greaterThanOrEqualTo(raiz.get("fechaRegistro"), filtro.desde()));
            if (filtro.hasta() != null) lista.add(cb.lessThanOrEqualTo(raiz.get("fechaRegistro"), filtro.hasta()));
            return cb.and(lista.toArray(Predicate[]::new));
        };
        return loteRepository.findAll(condiciones, Sort.by(Sort.Direction.DESC, "fechaRegistro", "id"));
    }

    /**
     * GET /api/lotes/exportar (HU11): los mismos lotes filtrados, en CSV.
     * Empieza con BOM UTF-8 para que Excel muestre bien las tildes.
     */
    @Transactional(readOnly = true)
    public byte[] exportarCsv(FiltroLotes filtro) {
        StringBuilder csv = new StringBuilder(BOM);
        csv.append("codigo,productor,origen,fecha_registro,cantidad_inicial_kg,cantidad_disponible_kg,"
                + "cantidad_consumida_kg,unidad,tipo_lote,estado").append(FIN_LINEA);
        for (Lote l : listar(filtro)) {
            BigDecimal consumida = l.getCantidadInicial().subtract(l.getCantidadDisponible());
            csv.append(String.join(",",
                    celda(l.getCodigo()),
                    celda(l.getProductor()),
                    celda(l.getOrigen()),
                    celda(String.valueOf(l.getFechaRegistro())),
                    celda(numero(l.getCantidadInicial())),
                    celda(numero(l.getCantidadDisponible())),
                    celda(numero(consumida)),
                    celda(l.getUnidad()),
                    celda(String.valueOf(l.getTipoLote())),
                    celda(String.valueOf(l.getEstado()))
            )).append(FIN_LINEA);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Marca de orden de bytes UTF-8: le indica a Excel que el archivo viene en UTF-8. */
    private static final String BOM = "﻿";
    /** RFC 4180 usa CRLF como fin de línea. */
    private static final String FIN_LINEA = "\r\n";

    /** Escapa una celda CSV (RFC 4180): entre comillas si trae coma, comillas o salto de línea. */
    private static String celda(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "" : AuditoriaService.cantidad(valor).toPlainString();
    }
}