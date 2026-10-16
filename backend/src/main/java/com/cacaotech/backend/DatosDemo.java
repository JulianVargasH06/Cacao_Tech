package com.cacaotech.backend;

import com.cacaotech.backend.dto.CostoRequestDto;
import com.cacaotech.backend.dto.IncidenciaRequestDto;
import com.cacaotech.backend.dto.LoteRequestDto;
import com.cacaotech.backend.dto.TransformacionRequestDto;
import com.cacaotech.backend.dto.TransformacionResponseDto;
import com.cacaotech.backend.model.Lote;
import com.cacaotech.backend.repository.LoteRepository;
import com.cacaotech.backend.service.CostoService;
import com.cacaotech.backend.service.EstadoLoteService;
import com.cacaotech.backend.service.IncidenciaService;
import com.cacaotech.backend.service.LoteService;
import com.cacaotech.backend.service.TransformacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

/**
 * Datos de demostración para la presentación. Solo se activa con el perfil "demo":
 * <pre>
 *   $env:SPRING_PROFILES_ACTIVE="demo"
 *   $env:DB_URL="jdbc:postgresql://localhost:5432/cacao_demo"   (base de datos aparte, vacía)
 * </pre>
 * Cuenta una historia coherente usando los mismos servicios de la aplicación, así que pasa por todas
 * sus reglas y deja auditoría, movimientos de inventario, alertas e historial de estados.
 * Las fechas son relativas al día en que se arranca, para que siempre se vean recientes.
 * Si ya existen los datos (CAC-2026-101), no hace nada. El lote CAC001 del jurado se revisa por separado.
 */
@Component
@Profile("demo")
public class DatosDemo {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);
    private static final String OPERADOR = "operador@cacaotech.com";
    private static final String ADMIN = "admin@cacaotech.com";

    private final LoteRepository loteRepository;
    private final LoteService loteService;
    private final TransformacionService transformacionService;
    private final CostoService costoService;
    private final IncidenciaService incidenciaService;
    private final EstadoLoteService estadoLoteService;

    public DatosDemo(LoteRepository loteRepository,
                     LoteService loteService,
                     TransformacionService transformacionService,
                     CostoService costoService,
                     IncidenciaService incidenciaService,
                     EstadoLoteService estadoLoteService) {
        this.loteRepository = loteRepository;
        this.loteService = loteService;
        this.transformacionService = transformacionService;
        this.costoService = costoService;
        this.incidenciaService = incidenciaService;
        this.estadoLoteService = estadoLoteService;
    }

    /** ApplicationReadyEvent: se ejecuta después del DataSeeder, cuando ya existen los usuarios de prueba. */
    @EventListener(ApplicationReadyEvent.class)
    public void cargar() {
        cargarLoteDelJurado();
        if (loteRepository.findByCodigo("CAC-2026-101").isPresent()) {
            log.info("Datos de demostración ya cargados: no se vuelven a crear");
            return;
        }
        log.info("Cargando datos de demostración...");

        // ─── 1. Recepción: cuatro fincas entregan cacao seco ─────────────────────────
        Lote esperanza = comoUsuario(OPERADOR, "OPERADOR", () ->
                recibir("CAC-2026-101", "Finca La Esperanza", "Rivera, Huila", 20, "500"));
        Lote porvenir = comoUsuario(OPERADOR, "OPERADOR", () ->
                recibir("CAC-2026-102", "Finca El Porvenir", "Gigante, Huila", 18, "350"));
        Lote santaRosa = comoUsuario(OPERADOR, "OPERADOR", () ->
                recibir("CAC-2026-103", "Finca Santa Rosa", "Algeciras, Huila", 16, "280"));
        comoUsuario(OPERADOR, "OPERADOR", () ->
                recibir("CAC-2026-104", "Asociación Los Andes", "Pitalito, Huila", 4, "420"));

        // ─── 2. Producto estrella: Finca La Esperanza, de la fermentación al empaque ─
        TransformacionResponseDto fermentacion = transformar("FERMENTACION", 15, 9,
                List.of(entrada(esperanza.getId(), "400")), "CAC-2026-101-FER", "360",
                "Fermentación en cajones de madera durante 6 días");
        TransformacionResponseDto secado = transformar("SECADO", 9, 14,
                List.of(entrada(salida(fermentacion), "360")), "CAC-2026-101-SEC", "317",
                "Secado al sol en marquesina");
        TransformacionResponseDto tostado = transformar("TOSTADO", 6, 10,
                List.of(entrada(salida(secado), "300")), "CAC-2026-101-TOS", "276", null);
        TransformacionResponseDto empaque = transformar("EMPAQUE", 2, 11,
                List.of(entrada(salida(tostado), "270")), "CAC-2026-101-EMP", "268",
                "Empaque en bolsas de 500 g para la venta");

        // ─── 3. Mezcla de dos fincas con un secado que pierde demasiado (alerta) ─────
        TransformacionResponseDto mezcla = transformar("FERMENTACION", 12, 8,
                List.of(entrada(porvenir.getId(), "300"), entrada(santaRosa.getId(), "200")),
                "CAC-2026-MEZ-01", "450", "Mezcla de dos fincas para un lote de exportación");
        TransformacionResponseDto secadoMezcla = transformar("SECADO", 7, 15,
                List.of(entrada(salida(mezcla), "450")), "CAC-2026-MEZ-01-SEC", "340",
                "Secado interrumpido por lluvias"); // 24 % de merma: alerta y bloqueo automático

        // ─── 4. Incidencia que explica la merma del secado ─────────────────────────
        comoUsuario(OPERADOR, "OPERADOR", () -> incidenciaService.registrar(new IncidenciaRequestDto(
                secadoMezcla.id(), salida(secadoMezcla), "HUMEDAD",
                "Lluvias durante tres días: el grano se rehumedeció y hubo que repetir el secado", false), OPERADOR));

        // ─── 5. Costos de proceso del producto estrella (HU05) ───────────────────────
        comoUsuario(ADMIN, "ADMINISTRADOR", () -> {
            costo(fermentacion, "MANO_DE_OBRA", "Volteo diario de los cajones (6 jornales)", "180000");
            costo(secado, "ENERGIA", "Secadora de apoyo en días nublados", "95000");
            costo(tostado, "ENERGIA", "Gas del tostador", "60000");
            costo(tostado, "MANO_DE_OBRA", "Operario de tostado", "90000");
            costo(empaque, "EMPAQUE", "536 bolsas de 500 g con etiqueta", "120000");
            costo(secadoMezcla, "MANO_DE_OBRA", "Repetición del secado por lluvia", "150000");
            return null;
        });

        // ─── 6. Control de calidad: el producto final queda liberado para la venta ──
        comoUsuario(ADMIN, "ADMINISTRADOR", () -> estadoLoteService.cambiarEstadoManual(
                salida(empaque), Lote.EstadoLote.LIBERADO,
                "Control de calidad aprobado: humedad 6 %, sin defectos", ADMIN));

        log.info("Datos de demostración listos. Producto para la ficha pública: CAC-2026-101-EMP");
    }

    /**
     * Lote CAC001, el que usa el jurado en la prueba "Bloquear CAC001 y consultar su ficha".
     * Va aparte de la historia principal para que también se cree en una base que ya tenía los demás datos.
     * Tiene una fermentación para que, al bloquearlo, se vea el impacto en el lote derivado.
     */
    private void cargarLoteDelJurado() {
        if (loteRepository.findByCodigo("CAC001").isPresent()) {
            return;
        }
        Lote cac001 = comoUsuario(OPERADOR, "OPERADOR", () ->
                recibir("CAC001", "Finca El Mirador", "Palermo, Huila", 10, "300"));
        transformar("FERMENTACION", 7, 9, List.of(entrada(cac001.getId(), "200")), "CAC001-FER", "185",
                "Fermentación en cajones durante 5 días");
        log.info("Lote CAC001 cargado para las pruebas del jurado");
    }

    // ─── Ayudas ──────────────────────────────────────────────────────────────────

    private Lote recibir(String codigo, String productor, String origen, int diasAtras, String kg) {
        return loteService.registrarRecepcion(new LoteRequestDto(codigo, productor, origen,
                LocalDate.now().minusDays(diasAtras), new BigDecimal(kg), "KG"));
    }

    private TransformacionResponseDto transformar(String tipo, int diasAtras, int hora,
                                                  List<TransformacionRequestDto.Entrada> entradas,
                                                  String codigoSalida, String kgSalida, String observaciones) {
        LocalDateTime fecha = LocalDate.now().minusDays(diasAtras).atTime(LocalTime.of(hora, 0));
        TransformacionRequestDto dto = new TransformacionRequestDto(tipo, fecha, entradas,
                List.of(new TransformacionRequestDto.Salida(codigoSalida, new BigDecimal(kgSalida))), observaciones);
        return comoUsuario(OPERADOR, "OPERADOR", () -> transformacionService.registrar(dto, OPERADOR));
    }

    private void costo(TransformacionResponseDto t, String categoria, String descripcion, String valor) {
        costoService.registrar(t.id(), new CostoRequestDto(categoria, descripcion, new BigDecimal(valor)), ADMIN);
    }

    private static TransformacionRequestDto.Entrada entrada(Long loteId, String kg) {
        return new TransformacionRequestDto.Entrada(loteId, new BigDecimal(kg));
    }

    /** Id del lote resultante de una transformación. */
    private static Long salida(TransformacionResponseDto t) {
        return t.salidas().get(0).loteId();
    }

    /**
     * Ejecuta como si ese usuario hubiera iniciado sesión, para que la auditoría muestre
     * su nombre y no "sistema" (AuditoriaService toma el usuario del contexto de seguridad).
     */
    private static <T> T comoUsuario(String correo, String rol, Supplier<T> accion) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        try {
            return accion.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
