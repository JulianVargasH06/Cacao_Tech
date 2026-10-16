package com.cacaotech.backend.unit.venta;

import com.cacaotech.backend.venta.MargenCalculadora;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias para {@link MargenCalculadora}.
 *
 * Sin contexto de Spring ni conexión a base de datos.
 * Verifica casos de cálculo exacto, redondeo, márgenes negativos por venta a pérdida,
 * escala final de 2 decimales y control de excepciones.
 */
@DisplayName("MargenCalculadora — pruebas unitarias")
class MargenVentaTest {

    private MargenCalculadora calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new MargenCalculadora();
    }

    // ─── Casos válidos de cálculo ─────────────────────────────────────────────

    @Test
    @DisplayName("1. Caso de referencia: precio 25.000 y costo 20.000 → margen del 20.00 %")
    void debeCalcularMargenSegunCasoDeReferencia() {
        BigDecimal precio = new BigDecimal("25000.00");
        BigDecimal costo = new BigDecimal("20000.00");

        BigDecimal margen = calculadora.calcularMargen(precio, costo);

        assertThat(margen).isEqualByComparingTo("20.00");
        assertThat(margen.scale()).as("Escala del resultado").isEqualTo(2);
    }

    @Test
    @DisplayName("2. Precio y costo iguales (15.000 y 15.000) → margen del 0.00 %")
    void debeRetornarMargenCeroCuandoPrecioYCostoSonIguales() {
        BigDecimal precio = new BigDecimal("15000.00");
        BigDecimal costo = new BigDecimal("15000.00");

        BigDecimal margen = calculadora.calcularMargen(precio, costo);

        assertThat(margen).isEqualByComparingTo("0.00");
        assertThat(margen.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("3. Costo unitario superior al precio (precio 70, costo 80) → margen negativo -14.29 %")
    void debePermitirMargenNegativoCuandoCostoSuperaPrecio() {
        BigDecimal precio = new BigDecimal("70.00");
        BigDecimal costo = new BigDecimal("80.00");

        BigDecimal margen = calculadora.calcularMargen(precio, costo);

        // (-10 / 70) * 100 = -14.2857142857... -> redondeo HALF_UP a 2 decimales = -14.29
        assertThat(margen).isEqualByComparingTo("-14.29");
        assertThat(margen.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("4. Costo unitario igual a cero (precio 10.000, costo 0) → margen del 100.00 %")
    void debeCalcularMargenCienPorCientoConCostoCero() {
        BigDecimal precio = new BigDecimal("10000.00");
        BigDecimal costo = BigDecimal.ZERO;

        BigDecimal margen = calculadora.calcularMargen(precio, costo);

        assertThat(margen).isEqualByComparingTo("100.00");
        assertThat(margen.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("5. Valores con decimales que requieran redondeo (precio 30, costo 20) → margen del 33.33 %")
    void debeRedondearCorrectamenteConDecimales() {
        BigDecimal precio = new BigDecimal("30.00");
        BigDecimal costo = new BigDecimal("20.00");

        BigDecimal margen = calculadora.calcularMargen(precio, costo);

        // (10 / 30) * 100 = 33.3333333333... -> redondeo HALF_UP a 2 decimales = 33.33
        assertThat(margen).isEqualByComparingTo("33.33");
        assertThat(margen.scale()).isEqualTo(2);
    }

    // ─── Validaciones y excepciones ───────────────────────────────────────────

    @Test
    @DisplayName("6. Precio de venta igual a cero → IllegalArgumentException")
    void debeRechazarPrecioVentaCero() {
        assertThatThrownBy(() -> calculadora.calcularMargen(BigDecimal.ZERO, new BigDecimal("1000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    @DisplayName("7. Precio de venta negativo → IllegalArgumentException")
    void debeRechazarPrecioVentaNegativo() {
        assertThatThrownBy(() -> calculadora.calcularMargen(new BigDecimal("-25000.00"), new BigDecimal("20000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    @DisplayName("8. Precio de venta nulo → IllegalArgumentException")
    void debeRechazarPrecioVentaNulo() {
        assertThatThrownBy(() -> calculadora.calcularMargen(null, new BigDecimal("20000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("9. Costo unitario nulo → IllegalArgumentException")
    void debeRechazarCostoUnitarioNulo() {
        assertThatThrownBy(() -> calculadora.calcularMargen(new BigDecimal("25000.00"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("10. Costo unitario negativo → IllegalArgumentException")
    void debeRechazarCostoUnitarioNegativo() {
        assertThatThrownBy(() -> calculadora.calcularMargen(new BigDecimal("25000.00"), new BigDecimal("-5000.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }
}
