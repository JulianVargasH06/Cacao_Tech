package com.cacaotech.backend;

import com.cacaotech.backend.dto.BloqueoIncidenteRequestDto;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de regresión del escenario de la hora 12 del reto:
 * "El comprador informa un problema con CAC001. El administrador debe bloquear el producto,
 * identificar su origen y explicar las cantidades afectadas. No debe borrarse el historial."
 *
 * Cadena de prueba (todo con códigos únicos por ejecución):
 * <pre>
 *   FINCA (200 kg) ─┬─ FERMENTACION 150 → FER (135) ── SECADO 135 → PRODUCTO (120, el reportado)
 *                   │                                      └─ TOSTADO 60 → DERIVADO (55)
 *                   └─ SECADO 40 → HERMANO (36)   (mismo origen, no sale del producto)
 * </pre>
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidenteHora12IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoteRepository loteRepository;

    private String operadorToken;
    private String adminToken;

    private long finca;
    private long fermentado;
    private long producto;
    private long derivado;
    private long hermano;
    private String codigoProducto;

    @BeforeEach
    void setUp() throws Exception {
        operadorToken = login("operador@cacaotech.com", "operador123");
        adminToken = login("admin@cacaotech.com", "admin123");

        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        codigoProducto = "CAC001-" + sufijo;
        finca = registrarLote("FINCA-" + sufijo, "200", 10);
        fermentado = transformar("FERMENTACION", 8, finca, "150", "FER-" + sufijo, "135");
        producto = transformar("SECADO", 5, fermentado, "135", codigoProducto, "120");
        derivado = transformar("TOSTADO", 2, producto, "60", "DER-" + sufijo, "55");
        hermano = transformar("SECADO", 3, finca, "40", "HER-" + sufijo, "36");
    }

    @Test
    @DisplayName("1. Identificar el origen y explicar las cantidades afectadas")
    void testImpacto() throws Exception {
        mockMvc.perform(get("/api/lotes/" + producto + "/impacto").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lote.codigo").value(codigoProducto))
                // Origen: la finca
                .andExpect(jsonPath("$.origenes", hasSize(1)))
                .andExpect(jsonPath("$.origenes[0].id").value(finca))
                // Afectados: el producto (60 kg en bodega) + su derivado (55 kg) = 115 kg
                .andExpect(jsonPath("$.afectados[*].id", containsInAnyOrder((int) producto, (int) derivado)))
                .andExpect(jsonPath("$.kgAfectadosDisponibles").value(115))
                // Mismo origen: la finca (10 kg sin procesar), el fermentado (0 kg) y el hermano (36 kg) = 46 kg
                .andExpect(jsonPath("$.relacionados[*].id",
                        containsInAnyOrder((int) finca, (int) fermentado, (int) hermano)))
                .andExpect(jsonPath("$.kgRelacionadosDisponibles").value(46))
                .andExpect(jsonPath("$.transformaciones", hasSize(4)));
    }

    @Test
    @DisplayName("2. Bloquear el producto y sus derivados, sin tocar los relacionados")
    void testBloqueoProductoYDerivados() throws Exception {
        bloquear(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueados", hasSize(2)))
                .andExpect(jsonPath("$.kgRetenidos").value(115))
                .andExpect(jsonPath("$.alertaId").isNumber());

        assertThat(estado(producto)).isEqualTo(Lote.EstadoLote.BLOQUEADO);
        assertThat(estado(derivado)).isEqualTo(Lote.EstadoLote.BLOQUEADO);
        assertThat(estado(hermano)).isEqualTo(Lote.EstadoLote.PENDIENTE);
        assertThat(estado(finca)).isEqualTo(Lote.EstadoLote.PENDIENTE);

        // Un lote bloqueado ya no se puede seguir transformando
        mockMvc.perform(post("/api/transformaciones")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(solicitud("EMPAQUE", 0, producto, "10", "X-" + codigoProducto, "9"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("está bloqueado")));
    }

    @Test
    @DisplayName("3. Con incluirRelacionados también se retiene lo que comparte el origen")
    void testBloqueoConRelacionados() throws Exception {
        bloquear(true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloqueados", hasSize(5)))
                .andExpect(jsonPath("$.kgRetenidos").value(161)); // 115 afectados + 46 relacionados

        for (long id : List.of(producto, derivado, finca, fermentado, hermano)) {
            assertThat(estado(id)).isEqualTo(Lote.EstadoLote.BLOQUEADO);
        }
    }

    @Test
    @DisplayName("4. No se borra el historial: los lotes, el recorrido y la auditoría siguen completos")
    void testNoSeBorraElHistorial() throws Exception {
        long lotesAntes = loteRepository.count();
        bloquear(false).andExpect(status().isOk());

        assertThat(loteRepository.count()).isEqualTo(lotesAntes);

        // El historial de estados conserva el bloqueo con su motivo
        mockMvc.perform(get("/api/lotes/" + producto + "/historial-estados").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$[0].estadoNuevo").value("BLOQUEADO"))
                .andExpect(jsonPath("$[0].motivo").value(containsString("sabor rancio")));

        // La auditoría registra el cambio de estado
        mockMvc.perform(get("/api/auditoria").param("entidad", "Lote").param("entidadId", String.valueOf(producto))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$[*].accion", hasItem("CAMBIAR_ESTADO")))
                .andExpect(jsonPath("$[*].accion", hasItem("CREAR")));

        // La ficha pública sigue mostrando el recorrido completo, ahora "Bloqueado · en revisión de calidad"
        mockMvc.perform(get("/api/publico/productos/" + codigoProducto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoDescripcion").value("Bloqueado · en revisión de calidad"))
                .andExpect(jsonPath("$.recorrido", hasSize(3)));

        // Y queda una alerta de retiro explicable
        mockMvc.perform(get("/api/alertas").param("estado", "ACTIVA").header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$[?(@.tipo == 'RETIRO')].descripcion",
                        hasItem(containsString("Retiro por incidente en " + codigoProducto))));
    }

    @Test
    @DisplayName("5. Solo el administrador; el motivo es obligatorio")
    void testPermisosYValidacion() throws Exception {
        mockMvc.perform(get("/api/lotes/" + producto + "/impacto").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/lotes/" + producto + "/bloqueo-incidente")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BloqueoIncidenteRequestDto(" ", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.motivo").exists());
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private ResultActions bloquear(boolean incluirRelacionados) throws Exception {
        return mockMvc.perform(post("/api/lotes/" + producto + "/bloqueo-incidente")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BloqueoIncidenteRequestDto(
                        "El comprador reporta sabor rancio en el chocolate", incluirRelacionados))));
    }

    private Lote.EstadoLote estado(long id) {
        return loteRepository.findById(id).orElseThrow().getEstado();
    }

    private String login(String correo, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto(correo, password))))
                .andExpect(status().isOk())
                .andReturn();
        return leer(r).get("token").asText();
    }

    private long registrarLote(String codigo, String kg, int diasAtras) throws Exception {
        LoteRequestDto dto = new LoteRequestDto(codigo, "Finca de Prueba", "Rivera, Huila",
                LocalDate.now().minusDays(diasAtras), new BigDecimal(kg), "KG");
        MvcResult r = mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(r).get("id").asLong();
    }

    /** Registra la transformación y devuelve el id del lote resultante. */
    private long transformar(String tipo, int diasAtras, long entradaId, String kgEntrada, String codigoSalida, String kgSalida)
            throws Exception {
        MvcResult r = mockMvc.perform(post("/api/transformaciones")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(tipo, diasAtras, entradaId, kgEntrada, codigoSalida, kgSalida))))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(r).get("salidas").get(0).get("loteId").asLong();
    }

    private static TransformacionRequestDto solicitud(String tipo, int diasAtras, long entradaId, String kgEntrada,
                                                      String codigoSalida, String kgSalida) {
        LocalDateTime fecha = diasAtras == 0
                ? LocalDateTime.now().minusMinutes(1).withNano(0)
                : LocalDate.now().minusDays(diasAtras).atTime(10, 0);
        return new TransformacionRequestDto(tipo, fecha,
                List.of(new TransformacionRequestDto.Entrada(entradaId, new BigDecimal(kgEntrada))),
                List.of(new TransformacionRequestDto.Salida(codigoSalida, new BigDecimal(kgSalida))), null);
    }

    private JsonNode leer(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
