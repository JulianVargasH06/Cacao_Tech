package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Transformacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransformacionRepository extends JpaRepository<Transformacion, Long> {
    List<Transformacion> findAllByOrderByFechaDescIdDesc();
}
