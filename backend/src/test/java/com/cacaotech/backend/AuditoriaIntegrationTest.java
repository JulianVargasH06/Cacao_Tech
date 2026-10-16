package com.cacaotech.backend;

import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la auditoría (HU10): cada operación deja su registro y solo el administrador puede consultarlo.
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditoriaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String operadorToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        operadorToken = login("operador@cacaotech.com", "operador123");
        adminToken = login("admin@cacaotech.com", "admin123");
    }

    @Test
    @DisplayName("1. Registrar un lote deja un CREAR con el usuario y los valores del lote")
    void testRecepcionQuedaAuditada() throws Exception {
        String codigo = codigoUnico("AUD-LOTE");
        long loteId = registrarLote(codigo, "100");

        mockMvc.perform(get("/api/auditoria")
                        .param("entidad", "Lote")
                        .param("entidadId", String.valueOf(loteId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accion").value("CREAR"))
                .andExpect(jsonPath("$[0].usuario").value("Operador CacaoTech"))
                .andExpect(jsonPath("$[0].correoUsuario").value("operador@cacaotech.com"))
                .andExpect(jsonPath("$[0].valoresAnteriores").doesNotExist())
                .andExpect(jsonPath("$[0].valoresNuevos").value(containsString("\"codigo\":\"" + codigo + "\"")))
                .andExpect(jsonPath("$[0].valoresNuevos").value(containsString("\"cantidadInicial\":100")));
    }

    @Test
    @DisplayName("2. Una transformación audita el consumo (antes y después), el lote resultante y la transformación")
    void testTransformacionQuedaAuditada() throws Exception {
        long entradaId = registrarLote(codigoUnico("AUD-ENT"), "100");
        String codigoSalida = codigoUnico("AUD-SAL");

        MvcResult transformacion = mockMvc.perform(post("/api/transformaciones")
                        .header("Authorization", "Bearer " + operadorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransformacionRequestDto(
                                "TOSTADO",
                                LocalDateTime.now().minusMinutes(1).withNano(0),
                                List.of(new TransformacionRequestDto.Entrada(entradaId, new BigDecimal("40"))),
                                List.of(new TransformacionRequestDto.Salida(codigoSalida, new BigDecimal("36"))),
                                null))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cuerpo = objectMapper.readTree(transformacion.getResponse().getContentAsString());
        long transformacionId = cuerpo.get("id").asLong();
        long salidaId = cuerpo.get("salidas").get(0).get("loteId").asLong();

        // Consumo del lote de entrada: 100 → 60
        mockMvc.perform(get("/api/auditoria")
                        .param("entidad", "Lote").param("entidadId", String.valueOf(entradaId)).param("accion", "CONSUMO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].valoresAnteriores").value(containsString("\"cantidadDisponible\":100")))
                .andExpect(jsonPath("$[0].valoresNuevos").value(containsString("\"cantidadDisponible\":60")));

        // Lote resultante creado, con referencia a la transformación
        mockMvc.perform(get("/api/auditoria")
                        .param("entidad", "Lote").param("entidadId", String.valueOf(salidaId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accion").value("CREAR"))
                .andExpect(jsonPath("$[0].valoresNuevos").value(containsString("RESULTADO_TRANSFORMACION")));

        // La transformación, con su merma
        mockMvc.perform(get("/api/auditoria")
                        .param("entidad", "Transformacion").param("entidadId", String.valueOf(transformacionId))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].valoresNuevos").value(containsString("\"mermaKg\":4")));
    }

    @Test
    @DisplayName("3. Filtros por usuario y fechas")
    void testFiltrosUsuarioYFechas() throws Exception {
        long loteId = registrarLote(codigoUnico("AUD-FIL"), "10");
        String hoy = LocalDate.now().toString();
        String manana = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/auditoria")
                        .param("usuario", "operador@cacaotech.com").param("desde", hoy).param("hasta", hoy)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entidadId == " + loteId + ")]", not(empty())));

        mockMvc.perform(get("/api/auditoria")
                        .param("desde", manana)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entidadId == " + loteId + ")]", empty()));
    }

    @Test
    @DisplayName("4. El operador no puede consultar la auditoría: 403")
    void testOperadorSinPermiso() throws Exception {
        mockMvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + operadorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permiso para realizar esta operación"));
    }

    @Test
    @DisplayName("5. Sin token: 401")
    void testSinToken() throws Exception {
        mockMvc.perform(get("/api/auditoria"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private String login(String correo, String password) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(correo, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    /** Registra un lote por la API (como operador) para que la auditoría se genere igual que en uso real. */
    private long registrarLote(String codigo, String cantidad) throws Exception {
        LoteRequestDto dto = new LoteRequestDto(codigo, "Finca de Prueba", "Rivera, Huila",
                LocalDate.now(), new BigDecimal(cantidad), "KG");
        MvcResult resultado = mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + operadorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    private static String codigoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
