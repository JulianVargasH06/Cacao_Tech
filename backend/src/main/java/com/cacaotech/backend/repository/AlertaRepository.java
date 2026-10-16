package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findAllByOrderByCreadaEnDescIdDesc();
    List<Alerta> findByEstadoOrderByCreadaEnDescIdDesc(String estado);
    Optional<Alerta> findFirstByTransformacionIdAndTipo(Long transformacionId, String tipo);
}
