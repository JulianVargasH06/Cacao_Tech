package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Costo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CostoRepository extends JpaRepository<Costo, Long> {
    List<Costo> findByTransformacionIdOrderByFechaAscIdAsc(Long transformacionId);

    @Query("select coalesce(sum(c.valorCop), 0) from Costo c where c.transformacion.id = :transformacionId")
    BigDecimal totalPorTransformacion(@Param("transformacionId") Long transformacionId);
}
