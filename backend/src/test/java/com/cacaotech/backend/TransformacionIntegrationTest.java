package com.cacaotech.backend;

import com.cacaotech.backend.dto.LoginRequestDto;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de POST y GET /api/transformaciones (RF02).
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransformacionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoteRepository loteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String operadorToken;
    private Lote loteEntrada;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequestDto login = new LoginRequestDto("operador@cacaotech.com", "operador123");
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        operadorToken = json.get("token").asText();

        loteEntrada = crearLote(codigoUnico("TEST-ENT"), new BigDecimal("150.000"), Lote.EstadoLote.PENDIENTE);
    }

    @Test
    @DisplayName("1. Registro exitoso: 201, descuenta la entrada, crea el lote de salida y calcula la merma")
    void testRegistroExitoso() throws Exception {
        String codigoSalida = codigoUnico("TEST-SAL");

        enviar(solicitud(loteEntrada.getId(), "100", codigoSalida, "90"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(startsWith("TR-")))
                .andExpect(jsonPath("$.tipo").value("SECADO"))
                .andExpect(jsonPath("$.responsable").value("Operador CacaoTech"))
                .andExpect(jsonPath("$.entradas[0].codigoLote").value(loteEntrada.getCodigo()))
                .andExpect(jsonPath("$.salidas[0].codigo").value(codigoSalida))
                .andExpect(jsonPath("$.mermaKg").value(10))
                .andExpect(jsonPath("$.mermaPorcentaje").value(10.00));

        Lote entradaActualizada = loteRepository.findById(loteEntrada.getId()).orElseThrow();
        assertThat(entradaActualizada.getCantidadDisponible()).isEqualByComparingTo("50");

        Lote salida = loteRepository.findByCodigo(codigoSalida).orElseThrow();
        assertThat(salida.getTipoLote()).isEqualTo(Lote.TipoLote.RESULTADO_TRANSFORMACION);
        assertThat(salida.getCantidadInicial()).isEqualByComparingTo("90");
        assertThat(salida.getCantidadDisponible()).isEqualByComparingTo("90");
        assertThat(salida.getProductor()).isEqualTo(loteEntrada.getProductor());
    }

    @Test
    @DisplayName("2. Consumo excesivo: 400 y el lote no pierde cacao (HU08)")
    void testCantidadSuperaDisponible() throws Exception {
        enviar(solicitud(loteEntrada.getId(), "200", codigoUnico("TEST-SAL"), "150"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith("El lote " + loteEntrada.getCodigo() + " solo tiene 150")));

        assertThat(loteRepository.findById(loteEntrada.getId()).orElseThrow().getCantidadDisponible())
                .isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("3. Lote bloqueado: 400, no se puede transformar")
    void testLoteBloqueado() throws Exception {
        Lote bloqueado = crearLote(codigoUnico("TEST-BLQ"), new BigDecimal("80"), Lote.EstadoLote.BLOQUEADO);

        enviar(solicitud(bloqueado.getId(), "10", codigoUnico("TEST-SAL"), "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("El lote " + bloqueado.getCodigo() + " está bloqueado y no se puede transformar"));
    }

    @Test
    @DisplayName("4. Salida mayor que la entrada: 400 (MermaCalculadora)")
    void testSalidaMayorQueEntrada() throws Exception {
        enviar(solicitud(loteEntrada.getId(), "50", codigoUnico("TEST-SAL"), "60"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith("La salida")));
    }

    @Test
    @DisplayName("5. Código de salida repetido: 400 y no se descuenta nada")
    void testCodigoSalidaRepetido() throws Exception {
        enviar(solicitud(loteEntrada.getId(), "20", loteEntrada.getCodigo(), "18"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe un lote registrado con el código: " + loteEntrada.getCodigo()));

        assertThat(loteRepository.findById(loteEntrada.getId()).orElseThrow().getCantidadDisponible())
                .isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("6. Validación del cuerpo: sin entradas responde 400 con el detalle del campo")
    void testSinEntradas() throws Exception {
        TransformacionRequestDto dto = new TransformacionRequestDto(
                "SECADO", LocalDateTime.now().minusMinutes(1).withNano(0), List.of(),
                List.of(new TransformacionRequestDto.Salida(codigoUnico("TEST-SAL"), new BigDecimal("5"))), null);

        enviar(dto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.entradas").value("Debe haber al menos un lote de entrada"));
    }

    @Test
    @DisplayName("6b. Fecha anterior a la recepción del lote: 400 y no se descuenta nada")
    void testFechaAnteriorALaRecepcion() throws Exception {
        TransformacionRequestDto dto = new TransformacionRequestDto(
                "SECADO", LocalDateTime.now().minusDays(1).withNano(0),
                List.of(new TransformacionRequestDto.Entrada(loteEntrada.getId(), new BigDecimal("10"))),
                List.of(new TransformacionRequestDto.Salida(codigoUnico("TEST-SAL"), new BigDecimal("9"))), null);

        enviar(dto)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(startsWith(
                        "La transformación no puede ser anterior a la recepción del lote " + loteEntrada.getCodigo())));

        assertThat(loteRepository.findById(loteEntrada.getId()).orElseThrow().getCantidadDisponible())
                .isEqualByComparingTo("150");
    }

    @Test
    @DisplayName("7. Sin token: 401")
    void testSinToken() throws Exception {
        mockMvc.perform(post("/api/transformaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                solicitud(loteEntrada.getId(), "10", codigoUnico("TEST-SAL"), "9"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. GET /api/transformaciones incluye la transformación recién registrada")
    void testListar() throws Exception {
        MvcResult creada = enviar(solicitud(loteEntrada.getId(), "30", codigoUnico("TEST-SAL"), "27"))
                .andExpect(status().isCreated())
                .andReturn();
        String codigo = objectMapper.readTree(creada.getResponse().getContentAsString()).get("codigo").asText();

        mockMvc.perform(get("/api/transformaciones").header("Authorization", "Bearer " + operadorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(hasItem(codigo)));
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("9. Reenvío con la misma Idempotency-Key: 200 con la misma transformación y sin doble consumo")
    void testReenvioSinDobleConsumo() throws Exception {
        String clave = UUID.randomUUID().toString();
        TransformacionRequestDto dto = solicitud(loteEntrada.getId(), "100", codigoUnico("TEST-SAL"), "80");

        MvcResult primero = enviarConClave(dto, clave)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mermaPorcentaje").value(20.00))
                .andReturn();
        long id = objectMapper.readTree(primero.getResponse().getContentAsString()).get("id").asLong();

        enviarConClave(dto, clave)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        // 150 - 100: solo se descontó una vez
        assertThat(loteRepository.findById(loteEntrada.getId()).orElseThrow().getCantidadDisponible())
                .isEqualByComparingTo("50");
    }

    private ResultActions enviarConClave(TransformacionRequestDto dto, String clave) throws Exception {
        return mockMvc.perform(post("/api/transformaciones")
                .header("Authorization", "Bearer " + operadorToken)
                .header("Idempotency-Key", clave)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private ResultActions enviar(TransformacionRequestDto dto) throws Exception {
        return mockMvc.perform(post("/api/transformaciones")
                .header("Authorization", "Bearer " + operadorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    private TransformacionRequestDto solicitud(Long loteId, String entradaKg, String codigoSalida, String salidaKg) {
        return new TransformacionRequestDto(
                "secado",
                LocalDateTime.now().minusMinutes(1).withNano(0),
                List.of(new TransformacionRequestDto.Entrada(loteId, new BigDecimal(entradaKg))),
                List.of(new TransformacionRequestDto.Salida(codigoSalida, new BigDecimal(salidaKg))),
                "Prueba automática");
    }

    private Lote crearLote(String codigo, BigDecimal cantidad, Lote.EstadoLote estado) {
        Lote lote = new Lote();
        lote.setCodigo(codigo);
        lote.setProductor("Finca de Prueba");
        lote.setOrigen("Rivera, Huila");
        lote.setFechaRegistro(LocalDate.now());
        lote.setCantidadInicial(cantidad);
        lote.setCantidadDisponible(cantidad);
        lote.setUnidad("KG");
        lote.setTipoLote(Lote.TipoLote.RECEPCION);
        lote.setEstado(estado);
        return loteRepository.save(lote);
    }

    /** Los códigos aleatorios evitan chocar con datos que ya existan en la base de datos local. */
    private static String codigoUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
