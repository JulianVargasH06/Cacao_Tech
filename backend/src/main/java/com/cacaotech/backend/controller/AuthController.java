package com.cacaotech.backend.controller;

import com.cacaotech.backend.dto.AuthResponseDto;
import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.UsuarioRepository;
import com.cacaotech.backend.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String rolNombre = (usuario.getRol() != null) ? usuario.getRol().getNombre() : "OPERADOR";
        String token = jwtUtil.generateToken(usuario.getCorreo(), rolNombre);

        AuthResponseDto response = new AuthResponseDto(token, usuario.getCorreo(), rolNombre);
        return ResponseEntity.ok(response);
    }
}