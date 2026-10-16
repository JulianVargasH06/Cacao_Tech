package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.EntradaTransformacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntradaTransformacionRepository extends JpaRepository<EntradaTransformacion, Long> {
    List<EntradaTransformacion> findByTransformacionId(Long transformacionId);
}
