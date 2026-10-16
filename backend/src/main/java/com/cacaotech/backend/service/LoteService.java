package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.repository.LoteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Todos los lotes, del más reciente al más antiguo (inventario e historial del frontend). */
    @Transactional(readOnly = true)
    public List<Lote> listar() {
        return loteRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaRegistro", "id"));
    }
}