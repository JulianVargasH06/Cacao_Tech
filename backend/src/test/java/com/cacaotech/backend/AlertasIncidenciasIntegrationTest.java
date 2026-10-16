package com.cacaotech.backend;

import com.cacaotech.backend.dto.CambioEstadoRequestDto;
import com.cacaotech.backend.dto.IncidenciaRequestDto;
import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
import com.cacaotech.backend.model.Lote;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Paso 5: alertas por merma con bloqueo automático (HU04), incidencias (RF03),
 * resolución de alertas y liberar/bloquear lotes (HU09).
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertasIncidenciasIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoteRepository loteRepository;

    private String operadorToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        operadorToken = login("operador@cacaotech.com", "operador123");
        adminToken = login("admin@cacaotech.com", "admin123");
    }

    // ─── Alerta automática por merma ─────────────────────────────────────────────

    @Test
    @DisplayName("1. Merma de 20 % (> 15 %): genera alerta explicable y bloquea el lote resultante")
    void testMermaAltaGeneraAlertaYBloqueo() throws Exception {
        long entradaId = registrarLote("100");
        JsonNode t = transformar(entradaId, "100", "80").andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString().transform(this::leer);

        assertThat(t.get("alerta").isNull()).isFalse();
        assertThat(t.get("alerta").get("tipo").asText()).isEqualTo("MERMA_ALTA");
        assertThat(t.get("alerta").get("descripcion").asText())
                .contains("Merma de 20 %").contains("entraron 100 kg y salieron 80 kg").contains("umbral de 15 %");

        long salidaId = t.get("salidas").get(0).get("loteId").asLong();
        assertThat(loteRepository.findById(salidaId).orElseThrow().getEstado()).isEqualTo(Lote.EstadoLote.BLOQUEADO);

        // El bloqueo queda en el historial de estados con su motivo
        mockMvc.perform(get("/api/lotes/" + salidaId + "/historial-estados").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estadoAnterior").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("BLOQUEADO"))
                .andExpect(jsonPath("$[0].motivo").value(containsString("Bloqueo automático")));

        // Y aparece en las alertas activas del administrador
        mockMvc.perform(get("/api/alertas").param("estado", "ACTIVA").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(t.get("alerta").get("id").asInt())));
    }

    @Test
    @DisplayName("2. Merma de 10 %: sin alerta y el lote resultante queda PENDIENTE")
    void testMermaNormalSinAlerta() throws Exception {
        long entradaId = registrarLote("100");
        JsonNode t = transformar(entradaId, "100", "90").andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString().transform(this::leer);

        assertThat(t.get("alerta").isNull()).isTrue();
        long salidaId = t.get("salidas").get(0).get("loteId").asLong();
        assertThat(loteRepository.findById(salidaId).orElseThrow().getEstado()).isEqualTo(Lote.EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("3. Merma exactamente de 15 %: no supera el umbral, sin alerta")
    void testMermaEnElUmbral() throws Exception {
        long entradaId = registrarLote("100");
        transformar(entradaId, "100", "85")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alerta").doesNotExist());
    }

    // ─── Resolver alertas ────────────────────────────────────────────────────────

    @Test
    @DisplayName("4. El administrador resuelve una alerta; resolverla otra vez da 400; el operador no puede (403)")
    void testResolverAlerta() throws Exception {
        long entradaId = registrarLote("100");
        long alertaId = transformar(entradaId, "100", "70").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("alerta").get("id").asLong();

        mockMvc.perform(patch("/api/alertas/" + alertaId + "/resolver").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/alertas/" + alertaId + "/resolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTA"))
                .andExpect(jsonPath("$.resueltaPor").value("Administrador CacaoTech"))
                .andExpect(jsonPath("$.resueltaEn", notNullValue()));

        mockMvc.perform(patch("/api/alertas/" + alertaId + "/resolver").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La alerta " + alertaId + " ya está resuelta"));
    }

    @Test
    @DisplayName("5. El operador no puede consultar alertas: 403")
    void testOperadorNoVeAlertas() throws Exception {
        mockMvc.perform(get("/api/alertas").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isForbidden());
    }

    // ─── Liberar / bloquear (HU09) ───────────────────────────────────────────────

    @Test
    @DisplayName("6. El administrador libera un lote bloqueado por merma, con motivo")
    void testLiberarLoteBloqueado() throws Exception {
        long entradaId = registrarLote("100");
        long salidaId = transformar(entradaId, "100", "80").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("salidas").get(0).get("loteId").asLong();

        cambiarEstado(salidaId, Lote.EstadoLote.LIBERADO, "Revisado: la merma se debió a humedad alta del grano", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBERADO"));

        mockMvc.perform(get("/api/lotes/" + salidaId + "/historial-estados").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].estadoNuevo").value("LIBERADO"))
                .andExpect(jsonPath("$[0].usuario").value("Administrador CacaoTech"));
    }

    @Test
    @DisplayName("7. Cambio de estado: sin motivo 400, mismo estado 400, PENDIENTE 400, operador 403")
    void testValidacionesCambioEstado() throws Exception {
        long loteId = registrarLote("50");

        cambiarEstado(loteId, Lote.EstadoLote.BLOQUEADO, " ", adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.motivo").value("El motivo es obligatorio"));

        cambiarEstado(loteId, Lote.EstadoLote.PENDIENTE, "Volver a pendiente", adminToken)
                .andExpect(status().isBadRequest());

        cambiarEstado(loteId, Lote.EstadoLote.BLOQUEADO, "Humedad fuera de rango", operadorToken)
                .andExpect(status().isForbidden());

        cambiarEstado(loteId, Lote.EstadoLote.BLOQUEADO, "Humedad fuera de rango", adminToken)
                .andExpect(status().isOk());
        cambiarEstado(loteId, Lote.EstadoLote.BLOQUEADO, "Otra vez", adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("ya está BLOQUEADO")));
    }

    // ─── Incidencias ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("8. Incidencia sin bloqueo: se registra y el lote no cambia de estado")
    void testIncidenciaSinBloqueo() throws Exception {
        long entradaId = registrarLote("100");
        JsonNode t = transformar(entradaId, "40", "36").andReturn().getResponse().getContentAsString().transform(this::leer);

        registrarIncidencia(t.get("id").asLong(), entradaId, false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("HUMEDAD"))
                .andExpect(jsonPath("$.usuario").value("Operador CacaoTech"))
                .andExpect(jsonPath("$.alertaId").doesNotExist());

        assertThat(loteRepository.findById(entradaId).orElseThrow().getEstado()).isEqualTo(Lote.EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("9. Incidencia con bloqueo: bloquea el lote, genera alerta INCIDENCIA y aparece al listar")
    void testIncidenciaConBloqueo() throws Exception {
        long entradaId = registrarLote("100");
        long transformacionId = transformar(entradaId, "40", "36").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("id").asLong();

        registrarIncidencia(transformacionId, entradaId, true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertaId", notNullValue()));

        assertThat(loteRepository.findById(entradaId).orElseThrow().getEstado()).isEqualTo(Lote.EstadoLote.BLOQUEADO);

        mockMvc.perform(get("/api/incidencias").param("transformacionId", String.valueOf(transformacionId))
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Un lote bloqueado ya no se puede transformar (regla del Paso 4)
        transformar(entradaId, "10", "9")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("está bloqueado")));
    }

    @Test
    @DisplayName("10. Incidencia: bloquear sin lote o con transformación inexistente responde 400")
    void testIncidenciaInvalida() throws Exception {
        long entradaId = registrarLote("100");
        long transformacionId = transformar(entradaId, "40", "36").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("id").asLong();

        registrarIncidencia(transformacionId, null, true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Para bloquear un lote debes indicar cuál (loteId)"));

        registrarIncidencia(999_999_999L, entradaId, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La transformación 999999999 no existe"));
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private String login(String correo, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(correo, password))))
                .andExpect(status().isOk())
                .andReturn();
        return leer(r.getResponse().getContentAsString()).get("token").asText();
    }

    private long registrarLote(String cantidad) throws Exception {
        LoteRequestDto dto = new LoteRequestDto(codigoUnico("ALR"), "Finca de Prueba", "Rivera, Huila",
                LocalDate.now(), new BigDecimal(cantidad), "KG");
        MvcResult r = mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions transformar(long loteId, String entradaKg, String salidaKg) throws Exception {
        TransformacionRequestDto dto = new TransformacionRequestDto(
                "SECADO",
                LocalDateTime.now().minusMinutes(1).withNano(0),
                List.of(new TransformacionRequestDto.Entrada(loteId, new BigDecimal(entradaKg))),
                List.of(new TransformacionRequestDto.Salida(codigoUnico("ALR-SAL"), new BigDecimal(salidaKg))),
                null);
        return mockMvc.perform(post("/api/transformaciones")
                .header("Authorization", bearer(operadorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private ResultActions cambiarEstado(long loteId, Lote.EstadoLote estado, String motivo, String token) throws Exception {
        return mockMvc.perform(patch("/api/lotes/" + loteId + "/estado")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CambioEstadoRequestDto(estado, motivo))));
    }

    private ResultActions registrarIncidencia(long transformacionId, Long loteId, boolean bloquear) throws Exception {
        IncidenciaRequestDto dto = new IncidenciaRequestDto(transformacionId, loteId, "humedad",
                "El grano llegó con 9 % de humedad", bloquear);
        return mockMvc.perform(post("/api/incidencias")
                .header("Authorization", bearer(operadorToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private JsonNode leer(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String codigoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
