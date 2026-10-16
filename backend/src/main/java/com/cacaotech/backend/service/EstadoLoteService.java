package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.HistorialEstadoResponseDto;
import com.cacaotech.backend.model.HistorialEstado;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.HistorialEstadoRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Único punto para cambiar el estado de un lote (PENDIENTE, LIBERADO, BLOQUEADO).
 * Todo cambio, manual o automático, deja su motivo en HistorialEstado y un registro de auditoría.
 */
@Service
public class EstadoLoteService {

    private final LoteRepository loteRepository;
    private final HistorialEstadoRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public EstadoLoteService(LoteRepository loteRepository,
                             HistorialEstadoRepository historialRepository,
                             UsuarioRepository usuarioRepository,
                             AuditoriaService auditoriaService) {
        this.loteRepository = loteRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    /** PATCH /api/lotes/{id}/estado (HU09): el administrador libera o bloquea un lote con un motivo. */
    @Transactional
    public Lote cambiarEstadoManual(Long loteId, Lote.EstadoLote nuevo, String motivo, String correoUsuario) {
        if (nuevo == Lote.EstadoLote.PENDIENTE) {
            throw new IllegalArgumentException("Solo se puede cambiar un lote a LIBERADO o BLOQUEADO");
        }
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El lote " + loteId + " no existe"));
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoUsuario));
        if (lote.getEstado() == nuevo) {
            throw new IllegalArgumentException("El lote " + lote.getCodigo() + " ya está " + nuevo);
        }
        cambiarEstado(lote, nuevo, motivo, usuario);
        return lote;
    }

    /**
     * Cambia el estado y deja constancia. Lo usan el cambio manual y los bloqueos automáticos
     * (merma alta, incidencias). Debe llamarse dentro de una transacción.
     */
    @Transactional
    public void cambiarEstado(Lote lote, Lote.EstadoLote nuevo, String motivo, Usuario usuario) {
        Lote.EstadoLote anterior = lote.getEstado();
        lote.setEstado(nuevo);

        HistorialEstado historial = new HistorialEstado();
        historial.setLote(lote);
        historial.setEstadoAnterior(anterior != null ? anterior.name() : null);
        historial.setEstadoNuevo(nuevo.name());
        historial.setMotivo(motivo.trim());
        historial.setUsuario(usuario);
        historial.setFecha(LocalDateTime.now());
        historialRepository.save(historial);

        Map<String, Object> valoresNuevos = new LinkedHashMap<>();
        valoresNuevos.put("estado", nuevo);
        valoresNuevos.put("motivo", motivo.trim());
        auditoriaService.registrar("Lote", lote.getId(), "CAMBIAR_ESTADO",
                anterior != null ? Map.of("estado", anterior) : null, valoresNuevos);
    }

    /** GET /api/lotes/{id}/historial-estados: por qué y cuándo cambió de estado un lote. */
    @Transactional(readOnly = true)
    public List<HistorialEstadoResponseDto> historial(Long loteId) {
        if (!loteRepository.existsById(loteId)) {
            throw new IllegalArgumentException("El lote " + loteId + " no existe");
        }
        return historialRepository.findByLoteIdOrderByFechaDescIdDesc(loteId).stream()
                .map(h -> new HistorialEstadoResponseDto(
                        h.getId(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getMotivo(),
                        h.getUsuario().getNombre(), h.getFecha()))
                .toList();
    }
}
