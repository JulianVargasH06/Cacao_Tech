package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoteService {

    private final LoteRepository loteRepository;

    public LoteService(LoteRepository loteRepository) {
        this.loteRepository = loteRepository;
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

        return loteRepository.save(lote);
    }
}