package com.cacaotech.backend;

import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.repository.LoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoteRecepcionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiamos lotes de pruebas previas con código CAC-2026-001 si existen
        loteRepository.findByCodigo("CAC-2026-001").ifPresent(loteRepository::delete);

        // 1. Autenticación previa para obtener token JWT de Administrador
        LoginRequestDto loginRequest = new LoginRequestDto("admin@cacaotech.com", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        this.adminToken = jsonNode.get("token").asText();
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @DisplayName("1. Creación exitosa (POST /api/lotes/recepcion): Retorna 201 y cantidadDisponible igual a 150.500")
    void testCreacionLoteExitosa() throws Exception {
        LoteRequestDto dto = new LoteRequestDto(
                "CAC-2026-001",
                "Finca La Esperanza",
                "Rivera",
                LocalDate.of(2026, 10, 16),
                new BigDecimal("150.500"),
                "KG"
        );

        mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CAC-2026-001"))
                .andExpect(jsonPath("$.productor").value("Finca La Esperanza"))
                .andExpect(jsonPath("$.origen").value("Rivera"))
                .andExpect(jsonPath("$.cantidadInicial").value(150.500))
                .andExpect(jsonPath("$.cantidadDisponible").value(150.500))
                .andExpect(jsonPath("$.unidad").value("KG"))
                .andExpect(jsonPath("$.tipoLote").value("RECEPCION"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("2. Validación de Negocio (RF01): Cantidad cero y negativa es rechazada con 400 Bad Request")
    void testCantidadInvalidaRechazada() throws Exception {
        // Caso cantidad negativa (-10)
        LoteRequestDto dtoNegativo = new LoteRequestDto(
                "CAC-2026-NEG",
                "Finca La Esperanza",
                "Rivera",
                LocalDate.of(2026, 10, 16),
                new BigDecimal("-10.000"),
                "KG"
        );

        mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoNegativo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.cantidadInicial").exists());

        // Caso cantidad cero (0)
        LoteRequestDto dtoCero = new LoteRequestDto(
                "CAC-2026-ZERO",
                "Finca La Esperanza",
                "Rivera",
                LocalDate.of(2026, 10, 16),
                BigDecimal.ZERO,
                "KG"
        );

        mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoCero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.cantidadInicial").exists());
    }

    @Test
    @DisplayName("3. Validación de Negocio (RF01): Código duplicado es rechazado con 400 Bad Request")
    void testCodigoDuplicadoRechazado() throws Exception {
        LoteRequestDto dtoOriginal = new LoteRequestDto(
                "CAC-2026-001",
                "Finca La Esperanza",
                "Rivera",
                LocalDate.of(2026, 10, 16),
                new BigDecimal("150.500"),
                "KG"
        );

        // Primer registro exitoso
        mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoOriginal)))
                .andExpect(status().isCreated());

        // Segundo registro con mismo código debe ser rechazado
        mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoOriginal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe un lote registrado con el código: CAC-2026-001"));
    }

    @Test
    @DisplayName("4. Seguridad: Consumir sin Authorization header devuelve 401 Unauthorized")
    void testSeguridadSinTokenDevuelve401() throws Exception {
        LoteRequestDto dto = new LoteRequestDto(
                "CAC-2026-UNAUTH",
                "Finca La Esperanza",
                "Rivera",
                LocalDate.of(2026, 10, 16),
                new BigDecimal("100.000"),
                "KG"
        );

        mockMvc.perform(post("/api/lotes/recepcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
