package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.IncidenciaRequestDto;
import com.cacaotech.backend.dto.IncidenciaResponseDto;
import com.cacaotech.backend.model.Alerta;
import com.cacaotech.backend.model.Incidencia;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.IncidenciaRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.TransformacionRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Incidencias (RF03): hallazgos de calidad durante una transformación.
 * Opcionalmente bloquean el lote afectado y generan una alerta (RF06).
 */
@Service
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final TransformacionRepository transformacionRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoLoteService estadoLoteService;
    private final AlertaService alertaService;
    private final AuditoriaService auditoriaService;

    public IncidenciaService(IncidenciaRepository incidenciaRepository,
                             TransformacionRepository transformacionRepository,
                             LoteRepository loteRepository,
                             UsuarioRepository usuarioRepository,
                             EstadoLoteService estadoLoteService,
                             AlertaService alertaService,
                             AuditoriaService auditoriaService) {
        this.incidenciaRepository = incidenciaRepository;
        this.transformacionRepository = transformacionRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoLoteService = estadoLoteService;
        this.alertaService = alertaService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public IncidenciaResponseDto registrar(IncidenciaRequestDto dto, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoUsuario));
        Transformacion transformacion = transformacionRepository.findById(dto.transformacionId())
                .orElseThrow(() -> new IllegalArgumentException("La transformación " + dto.transformacionId() + " no existe"));
        Lote lote = null;
        if (dto.loteId() != null) {
            lote = loteRepository.findById(dto.loteId())
                    .orElseThrow(() -> new IllegalArgumentException("El lote " + dto.loteId() + " no existe"));
        }
        if (dto.bloquearLote() && lote == null) {
            throw new IllegalArgumentException("Para bloquear un lote debes indicar cuál (loteId)");
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setTransformacion(transformacion);
        incidencia.setLote(lote);
        incidencia.setTipo(dto.tipo().trim().toUpperCase());
        incidencia.setDescripcion(dto.descripcion().trim());
        incidencia.setFecha(LocalDateTime.now());
        incidencia.setUsuario(usuario);
        incidencia = incidenciaRepository.save(incidencia);

        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("transformacion", transformacion.getCodigo());
        valores.put("lote", lote != null ? lote.getCodigo() : null);
        valores.put("tipo", incidencia.getTipo());
        valores.put("descripcion", incidencia.getDescripcion());
        valores.put("bloquearLote", dto.bloquearLote());
        auditoriaService.registrar("Incidencia", incidencia.getId(), "CREAR", null, valores);

        Alerta alerta = null;
        if (dto.bloquearLote()) {
            alerta = alertaService.crear(AlertaService.TIPO_INCIDENCIA, "ALTA",
                    "Incidencia " + incidencia.getTipo() + " en " + transformacion.getCodigo() + " sobre el lote "
                            + lote.getCodigo() + ": " + incidencia.getDescripcion(),
                    lote, transformacion);
            if (lote.getEstado() != Lote.EstadoLote.BLOQUEADO) {
                estadoLoteService.cambiarEstado(lote, Lote.EstadoLote.BLOQUEADO,
                        "Bloqueo por la incidencia " + incidencia.getId() + " (" + incidencia.getTipo() + "): "
                                + incidencia.getDescripcion(),
                        usuario);
            }
        }

        return aRespuesta(incidencia, alerta != null ? alerta.getId() : null);
    }

    /** GET /api/incidencias: todas, o solo las de una transformación o un lote. */
    @Transactional(readOnly = true)
    public List<IncidenciaResponseDto> listar(Long transformacionId, Long loteId) {
        List<Incidencia> incidencias;
        if (transformacionId != null) {
            incidencias = incidenciaRepository.findByTransformacionIdOrderByFechaDescIdDesc(transformacionId);
        } else if (loteId != null) {
            incidencias = incidenciaRepository.findByLoteIdOrderByFechaDescIdDesc(loteId);
        } else {
            incidencias = incidenciaRepository.findAllByOrderByFechaDescIdDesc();
        }
        return incidencias.stream().map(i -> aRespuesta(i, null)).toList();
    }

    private static IncidenciaResponseDto aRespuesta(Incidencia i, Long alertaId) {
        return new IncidenciaResponseDto(
                i.getId(),
                i.getTransformacion().getId(),
                i.getTransformacion().getCodigo(),
                i.getLote() != null ? i.getLote().getId() : null,
                i.getLote() != null ? i.getLote().getCodigo() : null,
                i.getTipo(),
                i.getDescripcion(),
                i.getFecha(),
                i.getUsuario().getNombre(),
                alertaId);
    }
}
