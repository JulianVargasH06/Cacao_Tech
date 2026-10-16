package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
    List<HistorialEstado> findByLoteIdOrderByFechaDescIdDesc(Long loteId);
}
