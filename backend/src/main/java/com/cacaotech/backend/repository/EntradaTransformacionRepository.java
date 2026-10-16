package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.EntradaTransformacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntradaTransformacionRepository extends JpaRepository<EntradaTransformacion, Long> {
    List<EntradaTransformacion> findByTransformacionId(Long transformacionId);

    /** Transformaciones en las que se usó un lote: permite seguir la trazabilidad hacia adelante. */
    List<EntradaTransformacion> findByLoteId(Long loteId);
}
