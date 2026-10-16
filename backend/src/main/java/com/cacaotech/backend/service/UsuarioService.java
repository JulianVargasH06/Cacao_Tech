package com.cacaotech.backend.service;

import com.cacaotech.backend.dto.UsuarioRequestDto;
import com.cacaotech.backend.dto.UsuarioResponseDto;
import com.cacaotech.backend.dto.UsuarioUpdateDto;
import com.cacaotech.backend.model.Rol;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.RolRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestión de cuentas internas (HU14): crear, cambiar rol y activar/desactivar.
 * La contraseña se guarda con BCrypt y nunca sale en respuestas ni en la auditoría.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    /** GET /api/usuarios */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listar() {
        return usuarioRepository.findAll(Sort.by("nombre")).stream().map(UsuarioService::aRespuesta).toList();
    }

    /** POST /api/usuarios */
    @Transactional
    public UsuarioResponseDto crear(UsuarioRequestDto dto) {
        String correo = dto.correo().trim().toLowerCase();
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con el correo " + correo);
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(dto.password()));
        usuario.setRol(buscarRol(dto.rol()));
        usuario.setActivo(true);
        usuario.setCreadoEn(LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);

        auditoriaService.registrar("Usuario", usuario.getId(), "CREAR", null, valores(usuario));
        return aRespuesta(usuario);
    }

    /**
     * PATCH /api/usuarios/{id}: cambia rol y/o estado.
     * Un administrador no puede desactivarse ni quitarse el rol a sí mismo (el sistema quedaría sin administrador).
     */
    @Transactional
    public UsuarioResponseDto actualizar(Long id, UsuarioUpdateDto dto, String correoQuienCambia) {
        if (dto.rol() == null && dto.activo() == null) {
            throw new IllegalArgumentException("Indica el rol o el estado (activo) que quieres cambiar");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El usuario " + id + " no existe"));
        boolean esUnoMismo = usuario.getCorreo().equalsIgnoreCase(correoQuienCambia);
        if (esUnoMismo && Boolean.FALSE.equals(dto.activo())) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }
        if (esUnoMismo && dto.rol() != null && !dto.rol().equals(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("No puedes cambiar tu propio rol");
        }

        Map<String, Object> antes = valores(usuario);
        if (dto.rol() != null) usuario.setRol(buscarRol(dto.rol()));
        if (dto.activo() != null) usuario.setActivo(dto.activo());
        auditoriaService.registrar("Usuario", usuario.getId(), "ACTUALIZAR", antes, valores(usuario));
        return aRespuesta(usuario);
    }

    private Rol buscarRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("El rol " + nombre + " no existe"));
    }

    /** Lo que se guarda en la auditoría: sin contraseña. */
    private static Map<String, Object> valores(Usuario u) {
        Map<String, Object> valores = new LinkedHashMap<>();
        valores.put("nombre", u.getNombre());
        valores.put("correo", u.getCorreo());
        valores.put("rol", u.getRol().getNombre());
        valores.put("activo", u.getActivo());
        return valores;
    }

    private static UsuarioResponseDto aRespuesta(Usuario u) {
        return new UsuarioResponseDto(u.getId(), u.getNombre(), u.getCorreo(), u.getRol().getNombre(),
                Boolean.TRUE.equals(u.getActivo()), u.getCreadoEn());
    }
}
