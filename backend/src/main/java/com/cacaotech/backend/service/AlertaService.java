package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.AlertaResponseDto;
import com.cacaotech.backend.model.Alerta;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.AlertaRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Alertas explicables (HU04): por merma alta en una transformación o por una incidencia que bloqueó un lote. */
@Service
public class AlertaService {

    public static final String TIPO_MERMA_ALTA = "MERMA_ALTA";
    public static final String TIPO_INCIDENCIA = "INCIDENCIA";
    public static final String ACTIVA = "ACTIVA";
    public static final String RESUELTA = "RESUELTA";

    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public AlertaService(AlertaRepository alertaRepository,
                         UsuarioRepository usuarioRepository,
                         AuditoriaService auditoriaService) {
        this.alertaRepository = alertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Crea una alerta ACTIVA. Debe llamarse dentro de la transacción de la operación que la provoca. */
    @Transactional
    public Alerta crear(String tipo, String severidad, String descripcion, Lote lote, Transformacion transformacion) {
        Alerta alerta = new Alerta();
        alerta.setTipo(tipo);
        alerta.setSeveridad(severidad);
        alerta.setDescripcion(descripcion);
        alerta.setLote(lote);
        alerta.setTransformacion(transformacion);
        alerta.setEstado(ACTIVA);
        alerta.setCreadaEn(LocalDateTime.now());
        alerta = alertaRepository.save(alerta);

        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("tipo", tipo);
        valores.put("severidad", severidad);
        valores.put("descripcion", descripcion);
        auditoriaService.registrar("Alerta", alerta.getId(), "CREAR", null, valores);
        return alerta;
    }

    /** GET /api/alertas: todas, o solo las de un estado (ACTIVA / RESUELTA). */
    @Transactional(readOnly = true)
    public List<AlertaResponseDto> listar(String estado) {
        List<Alerta> alertas = (estado == null || estado.isBlank())
                ? alertaRepository.findAllByOrderByCreadaEnDescIdDesc()
                : alertaRepository.findByEstadoOrderByCreadaEnDescIdDesc(estado.trim().toUpperCase());
        return alertas.stream().map(AlertaService::aRespuesta).toList();
    }

    /** PATCH /api/alertas/{id}/resolver: el administrador la da por revisada. */
    @Transactional
    public AlertaResponseDto resolver(Long id, String correoUsuario) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La alerta " + id + " no existe"));
        if (RESUELTA.equals(alerta.getEstado())) {
            throw new IllegalArgumentException("La alerta " + id + " ya está resuelta");
        }
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoUsuario));

        alerta.setEstado(RESUELTA);
        alerta.setResueltaEn(LocalDateTime.now());
        alerta.setResueltaPor(usuario);
        auditoriaService.registrar("Alerta", alerta.getId(), "RESOLVER",
                Map.of("estado", ACTIVA), Map.of("estado", RESUELTA));
        return aRespuesta(alerta);
    }

    static AlertaResponseDto aRespuesta(Alerta a) {
        return new AlertaResponseDto(
                a.getId(),
                a.getTipo(),
                a.getSeveridad(),
                a.getDescripcion(),
                a.getLote() != null ? a.getLote().getId() : null,
                a.getLote() != null ? a.getLote().getCodigo() : null,
                a.getTransformacion() != null ? a.getTransformacion().getId() : null,
                a.getTransformacion() != null ? a.getTransformacion().getCodigo() : null,
                a.getEstado(),
                a.getCreadaEn(),
                a.getResueltaEn(),
                a.getResueltaPor() != null ? a.getResueltaPor().getNombre() : null);
    }
}
