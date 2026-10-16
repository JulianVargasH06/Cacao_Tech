package com.cacaotech.backend.security;

import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        String rolNombre = (usuario.getRol() != null && usuario.getRol().getNombre() != null)
                ? usuario.getRol().getNombre()
                : "OPERADOR";

        return new org.springframework.security.core.userdetails.User(
                usuario.getCorreo(),
                usuario.getPasswordHash(),
                Boolean.TRUE.equals(usuario.getActivo()),
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + rolNombre))
        );
    }
}
