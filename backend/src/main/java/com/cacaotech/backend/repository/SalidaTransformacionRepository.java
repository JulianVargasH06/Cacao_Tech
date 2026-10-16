package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.SalidaTransformacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalidaTransformacionRepository extends JpaRepository<SalidaTransformacion, Long> {
    List<SalidaTransformacion> findByTransformacionId(Long transformacionId);
}
