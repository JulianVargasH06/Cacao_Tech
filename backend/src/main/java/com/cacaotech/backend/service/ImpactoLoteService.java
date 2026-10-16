package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.BloqueoIncidenteRequestDto;
import com.cacaotech.backend.dto.BloqueoIncidenteResponseDto;
import com.cacaotech.backend.dto.ImpactoLoteDto;
import com.cacaotech.backend.model.Alerta;
import com.cacaotech.backend.model.EntradaTransformacion;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.SalidaTransformacion;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.EntradaTransformacionRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.SalidaTransformacionRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incidente de producto / retiro (escenario de la hora 12): cuando un comprador reporta un problema,
 * identifica el origen del lote, lo que está afectado y lo que comparte su mismo origen, con sus cantidades,
 * y permite bloquearlo todo en un paso. Nunca borra nada: cada bloqueo queda en el historial y la auditoría.
 */
@Service
public class ImpactoLoteService {

    public static final String TIPO_ALERTA_RETIRO = "RETIRO";

    private final LoteRepository loteRepository;
    private final EntradaTransformacionRepository entradaRepository;
    private final SalidaTransformacionRepository salidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoLoteService estadoLoteService;
    private final AlertaService alertaService;

    public ImpactoLoteService(LoteRepository loteRepository,
                              EntradaTransformacionRepository entradaRepository,
                              SalidaTransformacionRepository salidaRepository,
                              UsuarioRepository usuarioRepository,
                              EstadoLoteService estadoLoteService,
                              AlertaService alertaService) {
        this.loteRepository = loteRepository;
        this.entradaRepository = entradaRepository;
        this.salidaRepository = salidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadoLoteService = estadoLoteService;
        this.alertaService = alertaService;
    }

    /** GET /api/lotes/{id}/impacto */
    @Transactional(readOnly = true)
    public ImpactoLoteDto impacto(Long loteId) {
        Analisis a = analizar(buscarLote(loteId));
        return new ImpactoLoteDto(
                resumen(a.lote()),
                a.origenes().stream().map(ImpactoLoteService::resumen).toList(),
                a.afectados().stream().map(ImpactoLoteService::resumen).toList(),
                a.relacionados().stream().map(ImpactoLoteService::resumen).toList(),
                List.copyOf(a.transformaciones()),
                disponible(a.afectados()),
                disponible(a.relacionados()));
    }

    /**
     * POST /api/lotes/{id}/bloqueo-incidente: bloquea el lote reportado y todo lo que se produjo con él
     * (y, si se pide, lo que comparte su origen). Los que ya estaban bloqueados no se tocan.
     */
    @Transactional
    public BloqueoIncidenteResponseDto bloquearPorIncidente(Long loteId, BloqueoIncidenteRequestDto dto, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoUsuario));
        Lote reportado = buscarLote(loteId);
        Analisis a = analizar(reportado);

        List<Lote> objetivo = new ArrayList<>(a.afectados());
        if (dto.incluirRelacionados()) objetivo.addAll(a.relacionados());

        String motivo = "Incidente reportado sobre " + reportado.getCodigo() + ": " + dto.motivo().trim();
        List<String> bloqueados = new ArrayList<>();
        List<String> yaBloqueados = new ArrayList<>();
        for (Lote lote : objetivo) {
            if (lote.getEstado() == Lote.EstadoLote.BLOQUEADO) {
                yaBloqueados.add(lote.getCodigo());
            } else {
                estadoLoteService.cambiarEstado(lote, Lote.EstadoLote.BLOQUEADO, motivo, usuario);
                bloqueados.add(lote.getCodigo());
            }
        }

        BigDecimal kgRetenidos = disponible(objetivo);
        Alerta alerta = alertaService.crear(TIPO_ALERTA_RETIRO, "ALTA",
                "Retiro por incidente en " + reportado.getCodigo() + ": " + objetivo.size() + " lotes retenidos ("
                        + AuditoriaService.cantidad(kgRetenidos).toPlainString() + " kg en bodega). Origen: "
                        + String.join(", ", a.origenes().stream().map(Lote::getCodigo).toList())
                        + ". Motivo: " + dto.motivo().trim(),
                reportado, null);

        return new BloqueoIncidenteResponseDto(bloqueados, yaBloqueados, kgRetenidos, alerta.getId());
    }

    // ─── Recorrido del grafo de lotes ────────────────────────────────────────────

    /** Lote reportado, sus orígenes de finca, lo afectado y lo relacionado por compartir origen. */
    private record Analisis(Lote lote, List<Lote> origenes, List<Lote> afectados, List<Lote> relacionados,
                            Set<String> transformaciones) {}

    private Analisis analizar(Lote reportado) {
        Set<String> transformaciones = new LinkedHashSet<>();

        // Hacia atrás: hasta los lotes que no salieron de ninguna transformación (los de finca)
        Map<Long, Lote> origenes = new LinkedHashMap<>();
        Deque<Lote> pendientes = new ArrayDeque<>(List.of(reportado));
        Set<Long> vistos = new LinkedHashSet<>();
        while (!pendientes.isEmpty()) {
            Lote lote = pendientes.pop();
            if (!vistos.add(lote.getId())) continue;
            SalidaTransformacion salida = salidaRepository.findByLoteResultanteId(lote.getId()).orElse(null);
            if (salida == null) {
                origenes.put(lote.getId(), lote);
                continue;
            }
            transformaciones.add(salida.getTransformacion().getCodigo());
            entradaRepository.findByTransformacionId(salida.getTransformacion().getId())
                    .forEach(e -> pendientes.push(e.getLote()));
        }

        // Hacia adelante: el lote reportado y todo lo que se produjo a partir de él
        Map<Long, Lote> afectados = descendientes(List.of(reportado), transformaciones);

        // Mismo origen: todo lo que salió de esos lotes de finca y no está ya entre los afectados
        Map<Long, Lote> relacionados = descendientes(origenes.values(), transformaciones);
        relacionados.keySet().removeAll(afectados.keySet());

        return new Analisis(reportado, ordenar(origenes.values()), ordenar(afectados.values()),
                ordenar(relacionados.values()), transformaciones);
    }

    /** Los lotes dados más todos los que se produjeron a partir de ellos (directa o indirectamente). */
    private Map<Long, Lote> descendientes(Iterable<Lote> iniciales, Set<String> transformaciones) {
        Map<Long, Lote> encontrados = new LinkedHashMap<>();
        Deque<Lote> pendientes = new ArrayDeque<>();
        iniciales.forEach(pendientes::push);
        while (!pendientes.isEmpty()) {
            Lote lote = pendientes.pop();
            if (encontrados.putIfAbsent(lote.getId(), lote) != null) continue;
            for (EntradaTransformacion uso : entradaRepository.findByLoteId(lote.getId())) {
                Transformacion t = uso.getTransformacion();
                transformaciones.add(t.getCodigo());
                salidaRepository.findByTransformacionIdOrderByIdAsc(t.getId())
                        .forEach(s -> pendientes.push(s.getLoteResultante()));
            }
        }
        return encontrados;
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private Lote buscarLote(Long id) {
        return loteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El lote " + id + " no existe"));
    }

    private static List<Lote> ordenar(Iterable<Lote> lotes) {
        List<Lote> lista = new ArrayList<>();
        lotes.forEach(lista::add);
        lista.sort(Comparator.comparing(Lote::getFechaRegistro).thenComparing(Lote::getId));
        return lista;
    }

    private static BigDecimal disponible(List<Lote> lotes) {
        return lotes.stream().map(Lote::getCantidadDisponible).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ImpactoLoteDto.LoteResumen resumen(Lote l) {
        return new ImpactoLoteDto.LoteResumen(l.getId(), l.getCodigo(), l.getTipoLote().name(), l.getEstado().name(),
                l.getProductor(), l.getOrigen(), l.getCantidadInicial(), l.getCantidadDisponible());
    }
}
