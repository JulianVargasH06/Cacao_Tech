package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.SalidaTransformacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalidaTransformacionRepository extends JpaRepository<SalidaTransformacion, Long> {
    List<SalidaTransformacion> findByTransformacionId(Long transformacionId);

    /** La salida que creó un lote: permite saber de qué transformación vino (trazabilidad hacia atrás). */
    Optional<SalidaTransformacion> findByLoteResultanteId(Long loteResultanteId);
}
