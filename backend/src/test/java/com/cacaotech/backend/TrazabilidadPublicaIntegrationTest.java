package com.cacaotech.backend;

import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Consulta pública de trazabilidad (HU06): sin token, con el recorrido completo y sin información privada.
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrazabilidadPublicaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String operadorToken;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("operador@cacaotech.com", "operador123"))))
                .andExpect(status().isOk())
                .andReturn();
        operadorToken = objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("1. Producto con dos etapas: recorrido completo desde la finca, sin token y sin datos privados")
    void testRecorridoCompleto() throws Exception {
        String a = codigo("TRZ-A");
        String aSec = codigo("TRZ-SEC");
        String aTos = codigo("TRZ-TOS");
        long loteA = registrarLote(a, "Finca La Esperanza", "Rivera, Huila", 5, "100");
        long loteSec = transformar("SECADO", 3, List.of(entrada(loteA, "100")), aSec, "90");
        transformar("TOSTADO", 2, List.of(entrada(loteSec, "90")), aTos, "81");

        MvcResult r = mockMvc.perform(get("/api/publico/trazabilidad/" + aTos)) // sin Authorization
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(aTos))
                .andExpect(jsonPath("$.tipo").value("Producto transformado"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.estadoDescripcion").value("Pendiente de liberación"))
                .andExpect(jsonPath("$.productor").value("Finca La Esperanza"))
                .andExpect(jsonPath("$.origen").value("Rivera, Huila"))
                .andExpect(jsonPath("$.origenes", hasSize(1)))
                .andExpect(jsonPath("$.origenes[0].codigoLote").value(a))
                .andExpect(jsonPath("$.recorrido", hasSize(3)))
                .andExpect(jsonPath("$.recorrido[*].etapa", contains("RECEPCION", "SECADO", "TOSTADO")))
                .andExpect(jsonPath("$.recorrido[1].descripcion").value("Secado: " + a + " → " + aSec))
                .andReturn();

        assertSinDatosPrivados(r.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("2. Mezcla de dos fincas: la ficha muestra ambos orígenes")
    void testMezclaDeDosFincas() throws Exception {
        long loteB = registrarLote(codigo("TRZ-B"), "Finca El Porvenir", "Gigante, Huila", 6, "50");
        long loteC = registrarLote(codigo("TRZ-C"), "Finca Santa Rosa", "Algeciras, Huila", 4, "50");
        String mezcla = codigo("TRZ-MEZ");
        transformar("FERMENTACION", 2, List.of(entrada(loteB, "50"), entrada(loteC, "50")), mezcla, "90");

        mockMvc.perform(get("/api/publico/trazabilidad/" + mezcla))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origenes", hasSize(2)))
                .andExpect(jsonPath("$.productor").value("Finca El Porvenir, Finca Santa Rosa"))
                .andExpect(jsonPath("$.recorrido", hasSize(3)))
                .andExpect(jsonPath("$.recorrido[*].etapa", contains("RECEPCION", "RECEPCION", "FERMENTACION")));
    }

    @Test
    @DisplayName("3. Lote de finca por la ruta /productos: un solo paso (la recepción)")
    void testLoteDeFincaRutaProductos() throws Exception {
        String codigo = codigo("TRZ-RAIZ");
        registrarLote(codigo, "Finca La Esperanza", "Rivera, Huila", 1, "70");

        mockMvc.perform(get("/api/publico/productos/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Materia prima recibida"))
                .andExpect(jsonPath("$.recorrido", hasSize(1)))
                .andExpect(jsonPath("$.recorrido[0].etapa").value("RECEPCION"))
                .andExpect(jsonPath("$.recorrido[0].descripcion")
                        .value("Recepción del lote " + codigo + " de Finca La Esperanza, Rivera, Huila"));
    }

    @Test
    @DisplayName("4. Código inexistente: 404 con mensaje claro")
    void testCodigoInexistente() throws Exception {
        mockMvc.perform(get("/api/publico/trazabilidad/NO-EXISTE-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No encontramos un producto con el código NO-EXISTE-123"));
    }

    @Test
    @DisplayName("5. Producto bloqueado por merma: se ve 'Bloqueado · en revisión de calidad' pero no el motivo ni la merma")
    void testProductoBloqueadoSinMotivo() throws Exception {
        long lote = registrarLote(codigo("TRZ-BLQ"), "Finca La Esperanza", "Rivera, Huila", 3, "100");
        String salida = codigo("TRZ-BLQ-SEC");
        transformar("SECADO", 1, List.of(entrada(lote, "100")), salida, "70"); // 30 % de merma: alerta y bloqueo

        MvcResult r = mockMvc.perform(get("/api/publico/trazabilidad/" + salida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"))
                .andExpect(jsonPath("$.estadoDescripcion").value("Bloqueado · en revisión de calidad"))
                .andReturn();

        String cuerpo = r.getResponse().getContentAsString();
        assertSinDatosPrivados(cuerpo);
        assertThat(cuerpo).doesNotContain("Bloqueo", "Merma", "merma", "alerta");
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    /** HU06: el comprador no debe ver usuarios, cantidades internas, costos, incidencias ni motivos. */
    private static void assertSinDatosPrivados(String json) {
        assertThat(json).doesNotContain("Operador", "operador@", "responsable", "usuario",
                "cantidad", "costo", "incidencia", "motivo", "observaciones");
    }

    private long registrarLote(String codigo, String productor, String origen, int diasAtras, String kg) throws Exception {
        LoteRequestDto dto = new LoteRequestDto(codigo, productor, origen,
                LocalDate.now().minusDays(diasAtras), new BigDecimal(kg), "KG");
        MvcResult r = mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", "Bearer " + operadorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Registra una transformación y devuelve el id del lote resultante. */
    private long transformar(String tipo, int diasAtras, List<TransformacionRequestDto.Entrada> entradas,
                             String codigoSalida, String salidaKg) throws Exception {
        TransformacionRequestDto dto = new TransformacionRequestDto(
                tipo,
                LocalDateTime.now().minusDays(diasAtras).withNano(0),
                entradas,
                List.of(new TransformacionRequestDto.Salida(codigoSalida, new BigDecimal(salidaKg))),
                "Nota interna que el comprador no debe ver");
        MvcResult r = mockMvc.perform(post("/api/transformaciones")
                        .header("Authorization", "Bearer " + operadorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("salidas").get(0).get("loteId").asLong();
    }

    private static TransformacionRequestDto.Entrada entrada(long loteId, String kg) {
        return new TransformacionRequestDto.Entrada(loteId, new BigDecimal(kg));
    }

    private static String codigo(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 6);
    }
}
