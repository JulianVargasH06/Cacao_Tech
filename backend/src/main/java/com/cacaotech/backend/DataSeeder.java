package com.cacaotech.backend;

import com.cacaotech.backend.model.Rol;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.RolRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(RolRepository rolRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Rol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombre("ADMINISTRADOR");
                return rolRepository.save(r);
            });

            Rol rolOperador = rolRepository.findByNombre("OPERADOR").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombre("OPERADOR");
                return rolRepository.save(r);
            });

            if (usuarioRepository.findByCorreo("admin@cacaotech.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador CacaoTech");
                admin.setCorreo("admin@cacaotech.com");
                admin.setPasswordHash(passwordEncoder.encode("admin123")); // Contraseña segura hasheada
                admin.setRol(rolAdmin);
                admin.setActivo(true);
                admin.setCreadoEn(LocalDateTime.now());
                usuarioRepository.save(admin);
            }

            // Usuario de prueba para las páginas del operador (recepción, transformaciones, inventario)
            if (usuarioRepository.findByCorreo("operador@cacaotech.com").isEmpty()) {
                Usuario operador = new Usuario();
                operador.setNombre("Operador CacaoTech");
                operador.setCorreo("operador@cacaotech.com");
                operador.setPasswordHash(passwordEncoder.encode("operador123"));
                operador.setRol(rolOperador);
                operador.setActivo(true);
                operador.setCreadoEn(LocalDateTime.now());
                usuarioRepository.save(operador);
            }
        };
    }
}