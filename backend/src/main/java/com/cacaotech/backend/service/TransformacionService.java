package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.TransformacionRequestDto;
import com.cacaotech.backend.dto.TransformacionResponseDto;
import com.cacaotech.backend.model.EntradaTransformacion;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.MovimientoInventario;
import com.cacaotech.backend.model.SalidaTransformacion;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.EntradaTransformacionRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.MovimientoInventarioRepository;
import com.cacaotech.backend.repository.SalidaTransformacionRepository;
import com.cacaotech.backend.repository.TransformacionRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import com.cacaotech.backend.model.Alerta;
import com.cacaotech.backend.repository.AlertaRepository;
import com.cacaotech.backend.repository.CostoRepository;
import com.cacaotech.backend.transformacion.MermaAlertaEvaluador;
import com.cacaotech.backend.transformacion.MermaCalculadora;
import com.cacaotech.backend.transformacion.ResultadoMerma;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de transformaciones (RF02): consume inventario de los lotes de entrada
 * y crea un lote nuevo por cada salida. Todo ocurre en una sola transacción:
 * si algo falla, no se descuenta nada ni se crea ningún lote.
 */
@Service
public class TransformacionService {

    private static final String REFERENCIA = "TRANSFORMACION";

    private final TransformacionRepository transformacionRepository;
    private final EntradaTransformacionRepository entradaRepository;
    private final SalidaTransformacionRepository salidaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final AlertaService alertaService;
    private final AlertaRepository alertaRepository;
    private final CostoRepository costoRepository;
    private final EstadoLoteService estadoLoteService;
    private final MermaCalculadora mermaCalculadora = new MermaCalculadora();
    private final MermaAlertaEvaluador alertaEvaluador = new MermaAlertaEvaluador();

    public TransformacionService(TransformacionRepository transformacionRepository,
                                 EntradaTransformacionRepository entradaRepository,
                                 SalidaTransformacionRepository salidaRepository,
                                 MovimientoInventarioRepository movimientoRepository,
                                 LoteRepository loteRepository,
                                 UsuarioRepository usuarioRepository,
                                 AuditoriaService auditoriaService,
                                 AlertaService alertaService,
                                 AlertaRepository alertaRepository,
                                 CostoRepository costoRepository,
                                 EstadoLoteService estadoLoteService) {
        this.transformacionRepository = transformacionRepository;
        this.entradaRepository = entradaRepository;
        this.salidaRepository = salidaRepository;
        this.movimientoRepository = movimientoRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.alertaService = alertaService;
        this.alertaRepository = alertaRepository;
        this.costoRepository = costoRepository;
        this.estadoLoteService = estadoLoteService;
    }

    @Transactional
    public TransformacionResponseDto registrar(TransformacionRequestDto dto, String correoResponsable) {
        Usuario responsable = usuarioRepository.findByCorreo(correoResponsable)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoResponsable));

        // ─── 1. Validar todo antes de modificar nada ─────────────────────────────
        Map<Long, Lote> lotesEntrada = validarEntradas(dto.entradas(), dto.fecha());
        validarSalidas(dto.salidas());

        BigDecimal totalEntradas = dto.entradas().stream()
                .map(TransformacionRequestDto.Entrada::cantidadKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSalidas = dto.salidas().stream()
                .map(TransformacionRequestDto.Salida::cantidadKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Lanza IllegalArgumentException si la salida supera la entrada
        ResultadoMerma merma = mermaCalculadora.calcular(totalEntradas, totalSalidas);

        // ─── 2. Guardar la transformación ────────────────────────────────────────
        Transformacion transformacion = new Transformacion();
        // El código definitivo usa el id, que solo existe después de guardar: primero va uno temporal
        transformacion.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 20));
        transformacion.setTipo(dto.tipo().trim().toUpperCase());
        transformacion.setFecha(dto.fecha());
        transformacion.setResponsable(responsable);
        transformacion.setObservaciones(textoOpcional(dto.observaciones()));
        transformacion = transformacionRepository.save(transformacion);
        transformacion.setCodigo(String.format("TR-%05d", transformacion.getId()));

        // ─── 3. Consumir los lotes de entrada (RF08) ─────────────────────────────
        List<EntradaTransformacion> entradas = new ArrayList<>();
        for (TransformacionRequestDto.Entrada e : dto.entradas()) {
            Lote lote = lotesEntrada.get(e.loteId());
            BigDecimal disponibleAntes = lote.getCantidadDisponible();
            lote.setCantidadDisponible(disponibleAntes.subtract(e.cantidadKg()));
            auditoriaService.registrar("Lote", lote.getId(), "CONSUMO",
                    Map.of("cantidadDisponible", AuditoriaService.cantidad(disponibleAntes)),
                    Map.of("cantidadDisponible", AuditoriaService.cantidad(lote.getCantidadDisponible()),
                            "transformacion", transformacion.getCodigo()));

            EntradaTransformacion entrada = new EntradaTransformacion();
            entrada.setTransformacion(transformacion);
            entrada.setLote(lote);
            entrada.setCantidadKg(e.cantidadKg());
            entradas.add(entradaRepository.save(entrada));

            registrarMovimiento(lote, "CONSUMO", e.cantidadKg(), responsable, transformacion,
                    "Entrada a " + transformacion.getTipo() + " " + transformacion.getCodigo());
        }

        // ─── 4. Crear un lote nuevo por cada salida ──────────────────────────────
        Lote origen = lotesEntrada.get(dto.entradas().get(0).loteId());
        List<SalidaTransformacion> salidas = new ArrayList<>();
        for (TransformacionRequestDto.Salida s : dto.salidas()) {
            Lote resultante = new Lote();
            resultante.setCodigo(s.codigo().trim());
            resultante.setProductor(origen.getProductor());
            resultante.setOrigen(origen.getOrigen());
            resultante.setFechaRegistro(dto.fecha().toLocalDate());
            resultante.setCantidadInicial(s.cantidadKg());
            resultante.setCantidadDisponible(s.cantidadKg());
            resultante.setUnidad("KG");
            resultante.setTipoLote(Lote.TipoLote.RESULTADO_TRANSFORMACION);
            resultante.setEstado(Lote.EstadoLote.PENDIENTE);
            resultante = loteRepository.save(resultante);
            Map<String, Object> valoresResultante = LoteService.valoresDeLote(resultante);
            valoresResultante.put("transformacion", transformacion.getCodigo());
            auditoriaService.registrar("Lote", resultante.getId(), "CREAR", null, valoresResultante);

            SalidaTransformacion salida = new SalidaTransformacion();
            salida.setTransformacion(transformacion);
            salida.setLoteResultante(resultante);
            salida.setCantidadKg(s.cantidadKg());
            salidas.add(salidaRepository.save(salida));

            registrarMovimiento(resultante, "PRODUCCION", s.cantidadKg(), responsable, transformacion,
                    "Salida de " + transformacion.getTipo() + " " + transformacion.getCodigo());
        }

        auditoriaService.registrar("Transformacion", transformacion.getId(), "CREAR", null,
                valoresDeTransformacion(transformacion, dto, lotesEntrada, merma));

        // ─── 5. Alerta explicable y bloqueo automático si la merma supera el umbral (HU04) ───
        Alerta alerta = null;
        if (alertaEvaluador.superaUmbral(merma.mermaPorcentaje())) {
            alerta = alertaService.crear(AlertaService.TIPO_MERMA_ALTA, "ALTA",
                    descripcionMermaAlta(transformacion, totalEntradas, totalSalidas, merma), origen, transformacion);
            for (SalidaTransformacion salida : salidas) {
                estadoLoteService.cambiarEstado(salida.getLoteResultante(), Lote.EstadoLote.BLOQUEADO,
                        "Bloqueo automático por la alerta " + alerta.getId() + ": merma de "
                                + numero(merma.mermaPorcentaje()) + " % en " + transformacion.getCodigo(),
                        responsable);
            }
        }

        return aRespuesta(transformacion, entradas, salidas, merma, alerta);
    }

    /** Todas las transformaciones, de la más reciente a la más antigua (historial del frontend). */
    @Transactional(readOnly = true)
    public List<TransformacionResponseDto> listar() {
        return transformacionRepository.findAllByOrderByFechaDescIdDesc().stream()
                .map(t -> {
                    List<EntradaTransformacion> entradas = entradaRepository.findByTransformacionId(t.getId());
                    List<SalidaTransformacion> salidas = salidaRepository.findByTransformacionIdOrderByIdAsc(t.getId());
                    BigDecimal totalEntradas = entradas.stream()
                            .map(EntradaTransformacion::getCantidadKg).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalSalidas = salidas.stream()
                            .map(SalidaTransformacion::getCantidadKg).reduce(BigDecimal.ZERO, BigDecimal::add);
                    ResultadoMerma merma = totalEntradas.signum() > 0
                            ? mermaCalculadora.calcular(totalEntradas, totalSalidas)
                            : new ResultadoMerma(BigDecimal.ZERO, BigDecimal.ZERO);
                    Alerta alerta = alertaRepository
                            .findFirstByTransformacionIdAndTipo(t.getId(), AlertaService.TIPO_MERMA_ALTA)
                            .orElse(null);
                    return aRespuesta(t, entradas, salidas, merma, alerta);
                })
                .toList();
    }

    // ─── Validaciones ────────────────────────────────────────────────────────────

    /**
     * Cada lote debe existir, no estar bloqueado, no repetirse, tener cacao suficiente (HU08)
     * y haberse registrado antes de la transformación (no se puede procesar cacao que aún no llegaba).
     */
    private Map<Long, Lote> validarEntradas(List<TransformacionRequestDto.Entrada> entradas, LocalDateTime fecha) {
        Map<Long, Lote> lotes = new LinkedHashMap<>();
        for (TransformacionRequestDto.Entrada e : entradas) {
            if (lotes.containsKey(e.loteId())) {
                throw new IllegalArgumentException("El lote " + e.loteId() + " aparece más de una vez como entrada");
            }
            Lote lote = loteRepository.findById(e.loteId())
                    .orElseThrow(() -> new IllegalArgumentException("El lote de entrada " + e.loteId() + " no existe"));
            if (lote.getEstado() == Lote.EstadoLote.BLOQUEADO) {
                throw new IllegalArgumentException("El lote " + lote.getCodigo() + " está bloqueado y no se puede transformar");
            }
            if (fecha.toLocalDate().isBefore(lote.getFechaRegistro())) {
                throw new IllegalArgumentException("La transformación no puede ser anterior a la recepción del lote "
                        + lote.getCodigo() + " (" + lote.getFechaRegistro() + ")");
            }
            if (e.cantidadKg().compareTo(lote.getCantidadDisponible()) > 0) {
                throw new IllegalArgumentException("El lote " + lote.getCodigo() + " solo tiene "
                        + lote.getCantidadDisponible().stripTrailingZeros().toPlainString() + " kg disponibles");
            }
            lotes.put(e.loteId(), lote);
        }
        return lotes;
    }

    /** Los códigos de los lotes resultantes no se pueden repetir ni entre sí ni con lotes existentes. */
    private void validarSalidas(List<TransformacionRequestDto.Salida> salidas) {
        Set<String> codigos = new HashSet<>();
        for (TransformacionRequestDto.Salida s : salidas) {
            String codigo = s.codigo().trim();
            if (!codigos.add(codigo.toUpperCase())) {
                throw new IllegalArgumentException("El código " + codigo + " está repetido en las salidas");
            }
            if (loteRepository.findByCodigo(codigo).isPresent()) {
                throw new IllegalArgumentException("Ya existe un lote registrado con el código: " + codigo);
            }
        }
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private void registrarMovimiento(Lote lote, String tipo, BigDecimal cantidad, Usuario usuario,
                                     Transformacion transformacion, String motivo) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setLote(lote);
        movimiento.setTipo(tipo);
        movimiento.setCantidadKg(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUsuario(usuario);
        movimiento.setReferenciaTipo(REFERENCIA);
        movimiento.setReferenciaId(transformacion.getId());
        movimiento.setMotivo(motivo);
        movimientoRepository.save(movimiento);
    }

    private TransformacionResponseDto aRespuesta(Transformacion t, List<EntradaTransformacion> entradas,
                                                 List<SalidaTransformacion> salidas, ResultadoMerma merma,
                                                 Alerta alerta) {
        return new TransformacionResponseDto(
                t.getId(),
                t.getCodigo(),
                t.getTipo(),
                t.getFecha(),
                t.getResponsable().getNombre(),
                t.getObservaciones(),
                entradas.stream()
                        .map(e -> new TransformacionResponseDto.Entrada(
                                e.getLote().getId(), e.getLote().getCodigo(), e.getCantidadKg()))
                        .toList(),
                salidas.stream()
                        .map(s -> new TransformacionResponseDto.Salida(
                                s.getLoteResultante().getId(), s.getLoteResultante().getCodigo(), s.getCantidadKg(),
                                s.getCostoAsignadoCop(), CostoService.costoUnitario(s)))
                        .toList(),
                merma.mermaKg(),
                merma.mermaPorcentaje(),
                costoRepository.totalPorTransformacion(t.getId()),
                alerta == null ? null
                        : new TransformacionResponseDto.AlertaGenerada(alerta.getId(), alerta.getTipo(), alerta.getDescripcion())
        );
    }

    /** Resumen de la transformación para la auditoría: qué entró, qué salió y cuánta merma hubo. */
    private static Map<String, Object> valoresDeTransformacion(Transformacion t, TransformacionRequestDto dto,
                                                              Map<Long, Lote> lotesEntrada, ResultadoMerma merma) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("codigo", t.getCodigo());
        valores.put("tipo", t.getTipo());
        valores.put("fecha", t.getFecha());
        valores.put("entradas", dto.entradas().stream()
                .map(e -> Map.of("lote", lotesEntrada.get(e.loteId()).getCodigo(),
                        "cantidadKg", AuditoriaService.cantidad(e.cantidadKg())))
                .toList());
        valores.put("salidas", dto.salidas().stream()
                .map(s -> Map.of("lote", s.codigo().trim(), "cantidadKg", AuditoriaService.cantidad(s.cantidadKg())))
                .toList());
        valores.put("mermaKg", AuditoriaService.cantidad(merma.mermaKg()));
        valores.put("mermaPorcentaje", merma.mermaPorcentaje());
        return valores;
    }

    /** Explica la alerta en lenguaje de negocio: qué proceso, cuánto entró y salió, y qué umbral se superó. */
    private static String descripcionMermaAlta(Transformacion t, BigDecimal entrada, BigDecimal salida, ResultadoMerma merma) {
        return "Merma de " + numero(merma.mermaPorcentaje()) + " % en " + t.getTipo() + " " + t.getCodigo()
                + ": entraron " + numero(entrada) + " kg y salieron " + numero(salida) + " kg ("
                + numero(merma.mermaKg()) + " kg perdidos). Supera el umbral de "
                + numero(MermaAlertaEvaluador.UMBRAL) + " %, por eso los lotes resultantes quedaron bloqueados para revisión.";
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    private static String textoOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
