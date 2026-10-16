package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.FichaPublicaDto;
import com.cacaotech.backend.exception.RecursoNoEncontradoException;
import com.cacaotech.backend.model.EntradaTransformacion;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.SalidaTransformacion;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.repository.EntradaTransformacionRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.SalidaTransformacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trazabilidad pública (HU06 / RF04-RF05): reconstruye el recorrido de un producto
 * desde la finca de origen, recorriendo hacia atrás las transformaciones que lo produjeron.
 */
@Service
public class TrazabilidadService {

    private final LoteRepository loteRepository;
    private final SalidaTransformacionRepository salidaRepository;
    private final EntradaTransformacionRepository entradaRepository;

    public TrazabilidadService(LoteRepository loteRepository,
                               SalidaTransformacionRepository salidaRepository,
                               EntradaTransformacionRepository entradaRepository) {
        this.loteRepository = loteRepository;
        this.salidaRepository = salidaRepository;
        this.entradaRepository = entradaRepository;
    }

    @Transactional(readOnly = true)
    public FichaPublicaDto fichaPublica(String codigo) {
        Lote producto = loteRepository.findByCodigo(codigo.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No encontramos un producto con el código " + codigo.trim()));

        // ─── 1. Recorrer hacia atrás: lote → transformación que lo creó → sus lotes de entrada → ... ───
        Map<Long, Lote> origenes = new LinkedHashMap<>();
        Map<Long, Transformacion> transformaciones = new LinkedHashMap<>();
        Map<Long, List<EntradaTransformacion>> entradasPorTransformacion = new LinkedHashMap<>();
        Map<Long, List<String>> salidasPorTransformacion = new LinkedHashMap<>();

        Deque<Lote> pendientes = new ArrayDeque<>(List.of(producto));
        Set<Long> visitados = new HashSet<>(); // evita ciclos si los datos estuvieran mal
        while (!pendientes.isEmpty()) {
            Lote lote = pendientes.pop();
            if (!visitados.add(lote.getId())) continue;

            SalidaTransformacion salida = salidaRepository.findByLoteResultanteId(lote.getId()).orElse(null);
            if (salida == null) {
                origenes.put(lote.getId(), lote); // no salió de ninguna transformación: es un lote de finca
                continue;
            }
            Transformacion t = salida.getTransformacion();
            salidasPorTransformacion.computeIfAbsent(t.getId(), id -> new ArrayList<>()).add(lote.getCodigo());
            if (transformaciones.putIfAbsent(t.getId(), t) == null) {
                List<EntradaTransformacion> entradas = entradaRepository.findByTransformacionId(t.getId());
                entradasPorTransformacion.put(t.getId(), entradas);
                entradas.forEach(e -> pendientes.push(e.getLote()));
            }
        }

        // ─── 2. Armar el recorrido en orden cronológico ──────────────────────────────
        List<FichaPublicaDto.Paso> recorrido = new ArrayList<>();
        for (Lote origen : origenes.values()) {
            recorrido.add(new FichaPublicaDto.Paso(
                    origen.getFechaRegistro().atStartOfDay(),
                    "RECEPCION",
                    "Recepción del lote " + origen.getCodigo() + lugar(origen)));
        }
        for (Transformacion t : transformaciones.values()) {
            String desde = entradasPorTransformacion.get(t.getId()).stream()
                    .map(e -> e.getLote().getCodigo()).collect(Collectors.joining(", "));
            String hacia = String.join(", ", salidasPorTransformacion.get(t.getId()));
            recorrido.add(new FichaPublicaDto.Paso(
                    t.getFecha(),
                    t.getTipo(),
                    capitalizar(t.getTipo()) + ": " + desde + " → " + hacia));
        }
        recorrido.sort(Comparator.comparing(FichaPublicaDto.Paso::fecha));

        // El recorrido usa una pila, así que los orígenes salen en cualquier orden: se ordenan por fecha de recepción
        List<FichaPublicaDto.Origen> listaOrigenes = origenes.values().stream()
                .sorted(Comparator.comparing(Lote::getFechaRegistro).thenComparing(Lote::getCodigo))
                .map(o -> new FichaPublicaDto.Origen(o.getCodigo(), o.getProductor(), o.getOrigen(), o.getFechaRegistro()))
                .toList();

        return new FichaPublicaDto(
                producto.getCodigo(),
                producto.getEstado().name(),
                descripcionEstado(producto.getEstado()),
                producto.getTipoLote() == Lote.TipoLote.RECEPCION ? "Materia prima recibida" : "Producto transformado",
                producto.getFechaRegistro(),
                unir(listaOrigenes.stream().map(FichaPublicaDto.Origen::productor).toList()),
                unir(listaOrigenes.stream().map(FichaPublicaDto.Origen::origen).toList()),
                listaOrigenes,
                recorrido);
    }

    /** El comprador ve el estado en palabras simples; el motivo de un bloqueo es información interna. */
    private static String descripcionEstado(Lote.EstadoLote estado) {
        return switch (estado) {
            case LIBERADO -> "Liberado para la venta";
            case BLOQUEADO -> "En revisión de calidad";
            case PENDIENTE -> "Pendiente de liberación";
        };
    }

    private static String lugar(Lote lote) {
        List<String> partes = new ArrayList<>();
        if (lote.getProductor() != null && !lote.getProductor().isBlank()) partes.add(lote.getProductor());
        if (lote.getOrigen() != null && !lote.getOrigen().isBlank()) partes.add(lote.getOrigen());
        return partes.isEmpty() ? "" : " de " + String.join(", ", partes);
    }

    /** Une valores distintos y no vacíos: "Finca A, Finca B". */
    private static String unir(List<String> valores) {
        return valores.stream().filter(v -> v != null && !v.isBlank()).distinct().collect(Collectors.joining(", "));
    }

    /** "SECADO" → "Secado" */
    private static String capitalizar(String texto) {
        String minusculas = texto.toLowerCase();
        return Character.toUpperCase(minusculas.charAt(0)) + minusculas.substring(1);
    }
}
