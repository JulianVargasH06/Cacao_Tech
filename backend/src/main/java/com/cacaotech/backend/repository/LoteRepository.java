package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {
    Optional<Lote> findByCodigo(String codigo);
}
