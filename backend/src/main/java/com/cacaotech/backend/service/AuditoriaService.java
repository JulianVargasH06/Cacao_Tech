package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.AuditoriaResponseDto;
import com.cacaotech.backend.model.Auditoria;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.AuditoriaRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Auditoría de cambios (HU10 / RF10): quién cambió qué entidad, cuándo y cómo quedó.
 * Los servicios de negocio la llaman dentro de su propia transacción:
 * si la operación falla, tampoco queda registro de auditoría.
 */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository,
                            UsuarioRepository usuarioRepository,
                            ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra un cambio. El usuario se toma del token de la petición actual.
     *
     * @param valoresAnteriores estado antes del cambio (null al crear)
     * @param valoresNuevos     estado después del cambio
     */
    @Transactional
    public void registrar(String entidad, Long entidadId, String accion,
                          Map<String, ?> valoresAnteriores, Map<String, ?> valoresNuevos) {
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuarioActual());
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setAccion(accion);
        auditoria.setValoresAnteriores(aJson(valoresAnteriores));
        auditoria.setValoresNuevos(aJson(valoresNuevos));
        auditoria.setFecha(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }

    /**
     * GET /api/auditoria: todos los filtros son opcionales; los que lleguen se combinan con "y".
     *
     * @param usuario correo de quien hizo el cambio
     * @param desde   desde este día, inclusive
     * @param hasta   hasta este día, inclusive
     */
    @Transactional(readOnly = true)
    public List<AuditoriaResponseDto> listar(String entidad, Long entidadId, String accion,
                                             String usuario, LocalDate desde, LocalDate hasta) {
        Specification<Auditoria> filtros = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (tieneTexto(entidad)) condiciones.add(cb.equal(cb.lower(raiz.get("entidad")), entidad.trim().toLowerCase()));
            if (entidadId != null) condiciones.add(cb.equal(raiz.get("entidadId"), entidadId));
            if (tieneTexto(accion)) condiciones.add(cb.equal(cb.upper(raiz.get("accion")), accion.trim().toUpperCase()));
            if (tieneTexto(usuario)) condiciones.add(cb.equal(cb.lower(raiz.join("usuario").get("correo")), usuario.trim().toLowerCase()));
            if (desde != null) condiciones.add(cb.greaterThanOrEqualTo(raiz.get("fecha"), desde.atStartOfDay()));
            if (hasta != null) condiciones.add(cb.lessThan(raiz.get("fecha"), hasta.plusDays(1).atStartOfDay()));
            return cb.and(condiciones.toArray(Predicate[]::new));
        };

        return auditoriaRepository.findAll(filtros, Sort.by(Sort.Direction.DESC, "fecha", "id")).stream()
                .map(a -> new AuditoriaResponseDto(
                        a.getId(),
                        a.getUsuario() != null ? a.getUsuario().getNombre() : "sistema",
                        a.getUsuario() != null ? a.getUsuario().getCorreo() : null,
                        a.getEntidad(),
                        a.getEntidadId(),
                        a.getAccion(),
                        a.getValoresAnteriores(),
                        a.getValoresNuevos(),
                        a.getFecha()))
                .toList();
    }

    /** Cantidades sin ceros sobrantes en el JSON: 100.000 → 100, 150.500 → 150.5 */
    public static BigDecimal cantidad(BigDecimal valor) {
        if (valor == null) return null;
        BigDecimal limpio = valor.stripTrailingZeros();
        // stripTrailingZeros convierte 100 en 1E+2 (escala negativa): se vuelve a escala 0 para que quede "100"
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null; // operación del sistema (ej. DataSeeder)
        }
        return usuarioRepository.findByCorreo(auth.getName()).orElse(null);
    }

    private String aJson(Map<String, ?> valores) {
        if (valores == null) return null;
        try {
            return objectMapper.writeValueAsString(valores);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir la auditoría a JSON", e);
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
