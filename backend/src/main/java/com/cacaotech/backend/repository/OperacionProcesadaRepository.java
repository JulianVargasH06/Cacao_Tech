package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.OperacionProcesada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OperacionProcesadaRepository extends JpaRepository<OperacionProcesada, Long> {
    Optional<OperacionProcesada> findByClaveIdempotencia(String claveIdempotencia);
}
