package com.cacaotech.backend;

import com.cacaotech.backend.dto.CostoRequestDto;
import com.cacaotech.backend.dto.LoginRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
import com.cacaotech.backend.dto.UsuarioRequestDto;
import com.cacaotech.backend.dto.UsuarioUpdateDto;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que faltaba del backend: filtros y CSV de lotes (HU11), costos y margen (HU05) y usuarios (HU14).
 * Cada prueba corre en una transacción que se revierte al final: no deja datos en cacao_db.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompletarBackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String operadorToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        operadorToken = login("operador@cacaotech.com", "operador123").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("token").asText();
        adminToken = login("admin@cacaotech.com", "admin123").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("token").asText();
    }

    // ─── HU11: filtros y CSV ─────────────────────────────────────────────────────

    @Test
    @DisplayName("1. GET /api/lotes con filtros por código y estado")
    void testFiltrosDeLotes() throws Exception {
        String codigo = codigo("FIL");
        registrarLote(codigo, "40");

        mockMvc.perform(get("/api/lotes").param("codigo", codigo.toLowerCase()).param("estado", "PENDIENTE")
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(hasItem(codigo)));

        mockMvc.perform(get("/api/lotes").param("codigo", codigo).param("estado", "BLOQUEADO")
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));

        // Origen o productor (CP-12): "Rivera, Huila" y "Finca de Prueba", sin importar mayúsculas
        mockMvc.perform(get("/api/lotes").param("codigo", codigo).param("origen", "rivera")
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(hasItem(codigo)));
        mockMvc.perform(get("/api/lotes").param("codigo", codigo).param("origen", "FINCA DE PRUEBA")
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(hasItem(codigo)));
        mockMvc.perform(get("/api/lotes").param("codigo", codigo).param("origen", "Pitalito")
                        .header("Authorization", bearer(operadorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()));
    }

    @Test
    @DisplayName("2. Exportar CSV: archivo descargable con encabezado y filtros; el operador no puede (403)")
    void testExportarCsv() throws Exception {
        String codigo = codigo("CSV");
        registrarLote(codigo, "125.5");

        MvcResult r = mockMvc.perform(get("/api/lotes/exportar").param("codigo", codigo).param("origen", "Rivera")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"lotes-")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String csv = new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String[] lineas = csv.replace("﻿", "").split("\r\n");
        assertThat(lineas).hasSize(2); // encabezado + el lote filtrado
        assertThat(lineas[0]).startsWith("codigo,productor,origen,fecha_registro");
        assertThat(lineas[1]).startsWith(codigo + ",Finca de Prueba,\"Rivera, Huila\"") // la coma va entre comillas
                .contains(",125.5,125.5,0,KG,RECEPCION,PENDIENTE");

        mockMvc.perform(get("/api/lotes/exportar").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isForbidden());
    }

    // ─── HU05: costos y margen ───────────────────────────────────────────────────

    @Test
    @DisplayName("3. Costos: se reparten entre las salidas según sus kg y aparecen en la transformación")
    void testCostosRepartidos() throws Exception {
        long lote = registrarLote(codigo("COS"), "100");
        JsonNode t = transformacionConDosSalidas(lote);
        long tId = t.get("id").asLong();

        registrarCosto(tId, "MANO_DE_OBRA", "Dos jornales de secado", "90000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costoTotalCop").value(90000))
                .andExpect(jsonPath("$.salidas[0].costoAsignadoCop").value(60000.00))
                .andExpect(jsonPath("$.salidas[1].costoAsignadoCop").value(30000.00))
                .andExpect(jsonPath("$.salidas[0].costoUnitarioCop").value(1000.00));

        registrarCosto(tId, "energia", "Secadora eléctrica", "9000")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costoTotalCop").value(99000))
                .andExpect(jsonPath("$.costos[1].categoria").value("ENERGIA"))
                .andExpect(jsonPath("$.salidas[0].costoAsignadoCop").value(66000.00))
                .andExpect(jsonPath("$.salidas[1].costoAsignadoCop").value(33000.00))
                .andExpect(jsonPath("$.salidas[1].costoUnitarioCop").value(1100.00));

        // El listado de transformaciones también trae los costos
        JsonNode lista = leer(mockMvc.perform(get("/api/transformaciones").header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getContentAsString());
        JsonNode esta = lista.findParents("codigo").stream()
                .filter(n -> n.has("costoTotalCop") && n.get("id").asLong() == tId).findFirst().orElseThrow();
        assertThat(esta.get("costoTotalCop").decimalValue()).isEqualByComparingTo("99000");
    }

    @Test
    @DisplayName("4. Margen sobre venta: (1500 - 1100) / 1500 = 26,67 %")
    void testMargen() throws Exception {
        long lote = registrarLote(codigo("MAR"), "100");
        JsonNode t = transformacionConDosSalidas(lote);
        registrarCosto(t.get("id").asLong(), "MANO_DE_OBRA", "Jornales", "99000");
        long salidaId = t.get("salidas").get(0).get("loteId").asLong();

        mockMvc.perform(get("/api/lotes/" + salidaId + "/margen").param("precioVentaKg", "1500")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costoUnitarioCop").value(1100.00))
                .andExpect(jsonPath("$.gananciaPorKgCop").value(400.00))
                .andExpect(jsonPath("$.margenPorcentaje").value(26.67));
    }

    @Test
    @DisplayName("5. Margen y costos inválidos: lote de finca, sin costos, valor 0 y operador")
    void testCostosYMargenInvalidos() throws Exception {
        long lote = registrarLote(codigo("INV"), "100");
        JsonNode t = transformacionConDosSalidas(lote);
        long salidaId = t.get("salidas").get(0).get("loteId").asLong();

        mockMvc.perform(get("/api/lotes/" + lote + "/margen").param("precioVentaKg", "1500")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("no salió de una transformación")));

        mockMvc.perform(get("/api/lotes/" + salidaId + "/margen").param("precioVentaKg", "1500")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("aún no tiene costos")));

        registrarCosto(t.get("id").asLong(), "EMPAQUE", "Bolsas", "0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.valorCop").value("El valor debe ser mayor a cero"));

        mockMvc.perform(post("/api/transformaciones/" + t.get("id").asLong() + "/costos")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CostoRequestDto("EMPAQUE", "Bolsas", new BigDecimal("5000")))))
                .andExpect(status().isForbidden());
    }

    // ─── HU14: usuarios ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("6. Crear usuario: 201 sin contraseña en la respuesta y puede iniciar sesión; correo repetido 400")
    void testCrearUsuario() throws Exception {
        String correo = codigo("nuevo").toLowerCase() + "@cacaotech.com";
        crearUsuario("Operaria Nueva", correo, "clave-segura-1", "OPERADOR")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.rol").value("OPERADOR"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        login(correo, "clave-segura-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Operaria Nueva"));

        crearUsuario("Otra", correo, "clave-segura-2", "OPERADOR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe un usuario con el correo " + correo));

        crearUsuario("Débil", codigo("debil").toLowerCase() + "@cacaotech.com", "123", "OPERADOR")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.password").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    @DisplayName("7. Desactivar: no puede iniciar sesión y su token deja de servir; al reactivar vuelve a entrar")
    void testDesactivarUsuario() throws Exception {
        String correo = codigo("baja").toLowerCase() + "@cacaotech.com";
        long id = crearUsuario("Operador Temporal", correo, "clave-segura-1", "OPERADOR").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("id").asLong();
        String tokenViejo = login(correo, "clave-segura-1").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("token").asText();

        actualizarUsuario(id, new UsuarioUpdateDto(null, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        login(correo, "clave-segura-1").andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/lotes").header("Authorization", bearer(tokenViejo)))
                .andExpect(status().isUnauthorized());

        actualizarUsuario(id, new UsuarioUpdateDto(null, true)).andExpect(status().isOk());
        login(correo, "clave-segura-1").andExpect(status().isOk());
    }

    @Test
    @DisplayName("8. Cambiar rol: el nuevo permiso aplica de inmediato, incluso con el token anterior")
    void testCambiarRol() throws Exception {
        String correo = codigo("rol").toLowerCase() + "@cacaotech.com";
        long id = crearUsuario("Futuro Admin", correo, "clave-segura-1", "OPERADOR").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("id").asLong();
        String token = login(correo, "clave-segura-1").andReturn()
                .getResponse().getContentAsString().transform(this::leer).get("token").asText();

        mockMvc.perform(get("/api/alertas").header("Authorization", bearer(token))).andExpect(status().isForbidden());

        actualizarUsuario(id, new UsuarioUpdateDto("ADMINISTRADOR", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));

        mockMvc.perform(get("/api/alertas").header("Authorization", bearer(token))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("9. Protecciones: el admin no se desactiva a sí mismo y el operador no gestiona usuarios")
    void testProteccionesUsuarios() throws Exception {
        // La lista incluye al admin y nunca expone contraseñas
        String usuarios = mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].correo").value(hasItem("admin@cacaotech.com")))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn().getResponse().getContentAsString();

        long adminId = leer(usuarios)
                .findParents("correo").stream()
                .filter(u -> u.get("correo").asText().equals("admin@cacaotech.com"))
                .findFirst().orElseThrow().get("id").asLong();

        actualizarUsuario(adminId, new UsuarioUpdateDto(null, false))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No puedes desactivar tu propia cuenta"));
        actualizarUsuario(adminId, new UsuarioUpdateDto("OPERADOR", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No puedes cambiar tu propio rol"));

        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(operadorToken)))
                .andExpect(status().isForbidden());
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private ResultActions login(String correo, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequestDto(correo, password))));
    }

    private long registrarLote(String codigo, String kg) throws Exception {
        LoteRequestDto dto = new LoteRequestDto(codigo, "Finca de Prueba", "Rivera, Huila",
                LocalDate.now(), new BigDecimal(kg), "KG");
        MvcResult r = mockMvc.perform(post("/api/lotes/recepcion")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(r.getResponse().getContentAsString()).get("id").asLong();
    }

    /** 100 kg de entrada → dos salidas de 60 y 30 kg (10 % de merma, sin alerta). */
    private JsonNode transformacionConDosSalidas(long loteId) throws Exception {
        TransformacionRequestDto dto = new TransformacionRequestDto(
                "SECADO",
                LocalDateTime.now().minusMinutes(1).withNano(0),
                List.of(new TransformacionRequestDto.Entrada(loteId, new BigDecimal("100"))),
                List.of(new TransformacionRequestDto.Salida(codigo("S60"), new BigDecimal("60")),
                        new TransformacionRequestDto.Salida(codigo("S30"), new BigDecimal("30"))),
                null);
        MvcResult r = mockMvc.perform(post("/api/transformaciones")
                        .header("Authorization", bearer(operadorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(r.getResponse().getContentAsString());
    }

    private ResultActions registrarCosto(long transformacionId, String categoria, String descripcion, String valor)
            throws Exception {
        return mockMvc.perform(post("/api/transformaciones/" + transformacionId + "/costos")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CostoRequestDto(categoria, descripcion, new BigDecimal(valor)))));
    }

    private ResultActions crearUsuario(String nombre, String correo, String password, String rol) throws Exception {
        return mockMvc.perform(post("/api/usuarios")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioRequestDto(nombre, correo, password, rol))));
    }

    private ResultActions actualizarUsuario(long id, UsuarioUpdateDto dto) throws Exception {
        return mockMvc.perform(patch("/api/usuarios/" + id)
                .header("Authorization", bearer(adminToken))
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

    private static String codigo(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
