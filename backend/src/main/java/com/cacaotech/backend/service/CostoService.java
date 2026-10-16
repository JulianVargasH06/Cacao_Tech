package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.CostoRequestDto;
import com.cacaotech.backend.dto.CostosTransformacionDto;
import com.cacaotech.backend.dto.MargenDto;
import com.cacaotech.backend.model.Costo;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.model.SalidaTransformacion;
import com.cacaotech.backend.model.Transformacion;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.CostoRepository;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.repository.SalidaTransformacionRepository;
import com.cacaotech.backend.repository.TransformacionRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import com.cacaotech.backend.transformacion.CostoDistribucionService;
import com.cacaotech.backend.transformacion.LoteSalida;
import com.cacaotech.backend.venta.MargenCalculadora;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Costos y margen (HU05): el administrador registra los costos de cada transformación;
 * se reparten entre los lotes resultantes según sus kg y de ahí sale el costo por kg y el margen sobre venta.
 */
@Service
public class CostoService {

    private final CostoRepository costoRepository;
    private final TransformacionRepository transformacionRepository;
    private final SalidaTransformacionRepository salidaRepository;
    private final LoteRepository loteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final CostoDistribucionService distribucion = new CostoDistribucionService();
    private final MargenCalculadora margenCalculadora = new MargenCalculadora();

    public CostoService(CostoRepository costoRepository,
                        TransformacionRepository transformacionRepository,
                        SalidaTransformacionRepository salidaRepository,
                        LoteRepository loteRepository,
                        UsuarioRepository usuarioRepository,
                        AuditoriaService auditoriaService) {
        this.costoRepository = costoRepository;
        this.transformacionRepository = transformacionRepository;
        this.salidaRepository = salidaRepository;
        this.loteRepository = loteRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    /** POST /api/transformaciones/{id}/costos: agrega un costo y vuelve a repartir el total entre las salidas. */
    @Transactional
    public CostosTransformacionDto registrar(Long transformacionId, CostoRequestDto dto, String correoUsuario) {
        Transformacion transformacion = buscarTransformacion(transformacionId);
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + correoUsuario));

        Costo costo = new Costo();
        costo.setTransformacion(transformacion);
        costo.setCategoria(dto.categoria().trim().toUpperCase());
        costo.setDescripcion(dto.descripcion().trim());
        costo.setValorCop(dto.valorCop());
        costo.setFecha(LocalDateTime.now());
        costo.setUsuario(usuario);
        costo = costoRepository.save(costo);

        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("transformacion", transformacion.getCodigo());
        valores.put("categoria", costo.getCategoria());
        valores.put("descripcion", costo.getDescripcion());
        valores.put("valorCop", AuditoriaService.cantidad(costo.getValorCop()));
        auditoriaService.registrar("Costo", costo.getId(), "CREAR", null, valores);

        repartir(transformacion);
        return consultar(transformacionId);
    }

    /** GET /api/transformaciones/{id}/costos */
    @Transactional(readOnly = true)
    public CostosTransformacionDto consultar(Long transformacionId) {
        Transformacion transformacion = buscarTransformacion(transformacionId);
        List<CostosTransformacionDto.Item> costos = costoRepository.findByTransformacionIdOrderByFechaAscIdAsc(transformacionId)
                .stream()
                .map(c -> new CostosTransformacionDto.Item(c.getId(), c.getCategoria(), c.getDescripcion(),
                        c.getValorCop(), c.getFecha(), c.getUsuario().getNombre()))
                .toList();
        List<CostosTransformacionDto.SalidaCosto> salidas = salidaRepository.findByTransformacionIdOrderByIdAsc(transformacionId)
                .stream()
                .map(s -> new CostosTransformacionDto.SalidaCosto(
                        s.getLoteResultante().getId(),
                        s.getLoteResultante().getCodigo(),
                        s.getCantidadKg(),
                        s.getCostoAsignadoCop(),
                        costoUnitario(s)))
                .toList();
        return new CostosTransformacionDto(transformacion.getId(), transformacion.getCodigo(),
                costoRepository.totalPorTransformacion(transformacionId), costos, salidas);
    }

    /** GET /api/lotes/{id}/margen?precioVentaKg=: margen sobre venta de un lote resultante. */
    @Transactional(readOnly = true)
    public MargenDto margen(Long loteId, BigDecimal precioVentaKg) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El lote " + loteId + " no existe"));
        SalidaTransformacion salida = salidaRepository.findByLoteResultanteId(loteId)
                .orElseThrow(() -> new IllegalArgumentException("El lote " + lote.getCodigo()
                        + " no salió de una transformación: no tiene costo de proceso asignado"));
        BigDecimal costoUnitario = costoUnitario(salida);
        if (costoUnitario == null) {
            throw new IllegalArgumentException("El lote " + lote.getCodigo()
                    + " aún no tiene costos: regístralos en su transformación " + salida.getTransformacion().getCodigo());
        }
        BigDecimal margen = margenCalculadora.calcularMargen(precioVentaKg, costoUnitario);
        return new MargenDto(lote.getId(), lote.getCodigo(), costoUnitario, precioVentaKg,
                precioVentaKg.subtract(costoUnitario).setScale(2, RoundingMode.HALF_UP), margen);
    }

    /** Reparte el costo total entre las salidas, proporcional a sus kg; la suma siempre da el total exacto. */
    private void repartir(Transformacion transformacion) {
        List<SalidaTransformacion> salidas = salidaRepository.findByTransformacionIdOrderByIdAsc(transformacion.getId());
        if (salidas.isEmpty()) return;
        BigDecimal total = costoRepository.totalPorTransformacion(transformacion.getId());
        List<BigDecimal> asignados = distribucion.distribuir(total, salidas.stream()
                .map(s -> new LoteSalida(s.getLoteResultante().getCodigo(), s.getCantidadKg()))
                .toList());
        for (int i = 0; i < salidas.size(); i++) {
            salidas.get(i).setCostoAsignadoCop(asignados.get(i));
        }
    }

    private Transformacion buscarTransformacion(Long id) {
        return transformacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La transformación " + id + " no existe"));
    }

    /** Costo por kg de una salida; null si todavía no tiene costos. */
    static BigDecimal costoUnitario(SalidaTransformacion salida) {
        if (salida.getCostoAsignadoCop() == null) return null;
        return salida.getCostoAsignadoCop().divide(salida.getCantidadKg(), 2, RoundingMode.HALF_UP);
    }
}
