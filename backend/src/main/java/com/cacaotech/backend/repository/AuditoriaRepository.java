package com.cacaotech.backend.repository;

import com.cacaotech.backend.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** JpaSpecificationExecutor permite armar la consulta con solo los filtros que llegaron (HU10). */
@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
}
