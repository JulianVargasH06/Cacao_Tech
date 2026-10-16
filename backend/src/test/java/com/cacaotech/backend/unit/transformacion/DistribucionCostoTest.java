package com.cacaotech.backend.unit.transformacion;

import com.cacaotech.backend.transformacion.CostoDistribucionService;
import com.cacaotech.backend.transformacion.LoteSalida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias para {@link CostoDistribucionService}.
 *
 * Sin contexto de Spring ni conexión a BD.
 * Valida proporcionalidad, conservación del costo total (suma exacta con last-cent adjustment)
 * y robustez ante entradas no válidas.
 */
@DisplayName("CostoDistribucionService — pruebas unitarias")
class DistribucionCostoTest {

    private CostoDistribucionService servicio;

    @BeforeEach
    void setUp() {
        servicio = new CostoDistribucionService();
    }

    // ─── Casos válidos de distribución y conservación ─────────────────────────

    @Test
    @DisplayName("CD-REF — Costo $1.600.000 entre 50 kg y 30 kg → $1.000.000 y $600.000")
    void debeDistribuirCostoSegunCasoDeReferencia() {
        BigDecimal costoTotal = new BigDecimal("1600000.00");
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-1", new BigDecimal("50")),
                new LoteSalida("LOTE-2", new BigDecimal("30"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(2);
        assertThat(distribucion.get(0)).isEqualByComparingTo("1000000.00");
        assertThat(distribucion.get(1)).isEqualByComparingTo("600000.00");

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).as("Suma de asignaciones").isEqualByComparingTo(costoTotal);
    }

    @Test
    @DisplayName("CD-01 — Proporciones iguales: 2 salidas de 40 kg c/u con costo $100.00 → $50.00 cada una")
    void debeDistribuirEquitativamenteCuandoLosPesosSonIguales() {
        BigDecimal costoTotal = new BigDecimal("100.00");
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-A", new BigDecimal("40")),
                new LoteSalida("LOTE-B", new BigDecimal("40"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(2);
        assertThat(distribucion.get(0)).isEqualByComparingTo("50.00");
        assertThat(distribucion.get(1)).isEqualByComparingTo("50.00");

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(costoTotal);
    }

    @Test
    @DisplayName("CD-02 — 3 salidas proporcionales (30, 30, 40 kg) con costo $100.00 → $30.00, $30.00 y $40.00")
    void debeDistribuirTresSalidasProporcionales() {
        BigDecimal costoTotal = new BigDecimal("100.00");
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-A", new BigDecimal("30")),
                new LoteSalida("LOTE-B", new BigDecimal("30")),
                new LoteSalida("LOTE-C", new BigDecimal("40"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(3);
        assertThat(distribucion.get(0)).isEqualByComparingTo("30.00");
        assertThat(distribucion.get(1)).isEqualByComparingTo("30.00");
        assertThat(distribucion.get(2)).isEqualByComparingTo("40.00");

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(costoTotal);
    }

    @Test
    @DisplayName("CD-03 — Ajuste de residuo por redondeo (3 salidas iguales de 10 kg con costo $10.00) → suma exacta $10.00")
    void debeAjustarResiduoEnUltimaAsignacionParaSumaExacta() {
        BigDecimal costoTotal = new BigDecimal("10.00");
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-1", new BigDecimal("10")),
                new LoteSalida("LOTE-2", new BigDecimal("10")),
                new LoteSalida("LOTE-3", new BigDecimal("10"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(3);
        // 10 / 30 = 0.3333333333 -> 10.00 * 0.3333333333 = 3.3333 -> round HALF_UP = 3.33
        // Lote 1: 3.33, Lote 2: 3.33, Lote 3 (last-cent): 10.00 - (3.33 + 3.33) = 3.34
        assertThat(distribucion.get(0)).isEqualByComparingTo("3.33");
        assertThat(distribucion.get(1)).isEqualByComparingTo("3.33");
        assertThat(distribucion.get(2)).isEqualByComparingTo("3.34");

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).as("La suma debe ser exactamente 10.00").isEqualByComparingTo(costoTotal);
    }

    @Test
    @DisplayName("CD-04 — Una sola salida: debe recibir el 100% del costo total con escala 2")
    void debeAsignarTodoElCostoAUnaSolaSalida() {
        BigDecimal costoTotal = new BigDecimal("500");
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-UNICO", new BigDecimal("80"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(1);
        assertThat(distribucion.get(0)).isEqualByComparingTo("500.00");
        assertThat(distribucion.get(0).scale()).isEqualTo(2);

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(costoTotal);
    }

    @Test
    @DisplayName("CD-05 — Costo total igual a cero → asignaciones en cero con escala 2 y suma exacta")
    void debeGenerarAsignacionesCeroConCostoTotalCero() {
        BigDecimal costoTotal = BigDecimal.ZERO;
        List<LoteSalida> salidas = List.of(
                new LoteSalida("LOTE-A", new BigDecimal("50")),
                new LoteSalida("LOTE-B", new BigDecimal("30"))
        );

        List<BigDecimal> distribucion = servicio.distribuir(costoTotal, salidas);

        assertThat(distribucion).hasSize(2);
        assertThat(distribucion.get(0)).isEqualByComparingTo("0.00");
        assertThat(distribucion.get(0).scale()).isEqualTo(2);
        assertThat(distribucion.get(1)).isEqualByComparingTo("0.00");
        assertThat(distribucion.get(1).scale()).isEqualTo(2);

        BigDecimal suma = distribucion.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── Validaciones y excepciones ───────────────────────────────────────────

    @Test
    @DisplayName("E-01 — Costo total negativo → IllegalArgumentException")
    void debeRechazarCostoTotalNegativo() {
        List<LoteSalida> salidas = List.of(new LoteSalida("LOTE-1", new BigDecimal("50")));

        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("-1.00"), salidas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("E-02 — Costo total nulo → IllegalArgumentException")
    void debeRechazarCostoTotalNulo() {
        List<LoteSalida> salidas = List.of(new LoteSalida("LOTE-1", new BigDecimal("50")));

        assertThatThrownBy(() -> servicio.distribuir(null, salidas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("E-03 — Lista de salidas nula → IllegalArgumentException")
    void debeRechazarListaSalidasNula() {
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nula");
    }

    @Test
    @DisplayName("E-04 — Lista de salidas vacía → IllegalArgumentException")
    void debeRechazarListaSalidasVacia() {
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacía");
    }

    @Test
    @DisplayName("E-05 — Elemento LoteSalida nulo en la lista → IllegalArgumentException")
    void debeRechazarElementoLoteNulo() {
        List<LoteSalida> salidas = new ArrayList<>();
        salidas.add(new LoteSalida("LOTE-1", new BigDecimal("50")));
        salidas.add(null);

        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("E-06 — Identificador de lote nulo o vacío → IllegalArgumentException")
    void debeRechazarIdentificadorLoteVacioONulo() {
        List<LoteSalida> salidasConIdVacio = List.of(new LoteSalida("   ", new BigDecimal("50")));
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidasConIdVacio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");

        List<LoteSalida> salidasConIdNulo = List.of(new LoteSalida(null, new BigDecimal("50")));
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidasConIdNulo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("E-07 — Kilogramos nulos en una salida → IllegalArgumentException")
    void debeRechazarKilogramosNulos() {
        List<LoteSalida> salidas = List.of(new LoteSalida("LOTE-1", null));

        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulos");
    }

    @Test
    @DisplayName("E-08 — Kilogramos cero o negativos en una salida → IllegalArgumentException")
    void debeRechazarKilogramosCeroONegativos() {
        List<LoteSalida> salidasCero = List.of(new LoteSalida("LOTE-1", BigDecimal.ZERO));
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidasCero))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivos");

        List<LoteSalida> salidasNegativas = List.of(new LoteSalida("LOTE-2", new BigDecimal("-5")));
        assertThatThrownBy(() -> servicio.distribuir(new BigDecimal("100"), salidasNegativas))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivos");
    }
}
