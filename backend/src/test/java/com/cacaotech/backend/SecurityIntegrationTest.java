package com.cacaotech.backend;

import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.model.Rol;
import com.cacaotech.backend.model.Usuario;
import com.cacaotech.backend.repository.RolRepository;
import com.cacaotech.backend.repository.UsuarioRepository;
import com.cacaotech.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("1. Verificación del Seeder: Roles y Usuario Administrador insertados")
    void testDataSeederSuccess() {
        // Verificar Rol ADMINISTRADOR
        Optional<Rol> rolAdmin = rolRepository.findByNombre("ADMINISTRADOR");
        assertThat(rolAdmin).isPresent();

        // Verificar Rol OPERADOR
        Optional<Rol> rolOperador = rolRepository.findByNombre("OPERADOR");
        assertThat(rolOperador).isPresent();

        // Verificar Usuario Administrador por defecto
        Optional<Usuario> usuarioAdminOpt = usuarioRepository.findByCorreo("admin@cacaotech.com");
        assertThat(usuarioAdminOpt).isPresent();

        Usuario admin = usuarioAdminOpt.get();
        assertThat(admin.getNombre()).isEqualTo("Administrador CacaoTech");
        assertThat(admin.getActivo()).isTrue();
        assertThat(admin.getRol().getNombre()).isEqualTo("ADMINISTRADOR");
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("2. Autenticación exitosa (POST /api/auth/login) retorna 200 y Token JWT válido")
    void testLoginExitoso() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto("admin@cacaotech.com", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.correo").value("admin@cacaotech.com"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("token").asText();

        // Validar el token con JwtUtil
        String username = jwtUtil.extractUsername(token);
        String rolClaim = jwtUtil.extractClaim(token, claims -> claims.get("rol", String.class));
        assertThat(username).isEqualTo("admin@cacaotech.com");
        assertThat(rolClaim).isEqualTo("ADMINISTRADOR");
    }

    @Test
    @DisplayName("3. Ruta pública (/api/publico/ping) responde 200 sin necesidad de token")
    void testRutaPublicaSinToken() throws Exception {
        mockMvc.perform(get("/api/publico/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. Ruta protegida (/api/protegido/ping) sin token devuelve 401 Unauthorized")
    void testRutaProtegidaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/protegido/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Ruta protegida (/api/protegido/ping) con token JWT válido devuelve 200 OK")
    void testRutaProtegidaConTokenValidoDevuelve200() throws Exception {
        // Obtenemos token mediante login
        LoginRequestDto loginRequest = new LoginRequestDto("admin@cacaotech.com", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = jsonNode.get("token").asText();

        // Accedemos a la ruta protegida enviando el token en el Header Authorization Bearer
        mockMvc.perform(get("/api/protegido/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Acceso concedido a ruta protegida de Cacao Tech"))
                .andExpect(jsonPath("$.usuario").value("admin@cacaotech.com"));
    }
}
