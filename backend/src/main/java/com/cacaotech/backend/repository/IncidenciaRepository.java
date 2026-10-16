package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {
    List<Incidencia> findAllByOrderByFechaDescIdDesc();
    List<Incidencia> findByTransformacionIdOrderByFechaDescIdDesc(Long transformacionId);
    List<Incidencia> findByLoteIdOrderByFechaDescIdDesc(Long loteId);
}
