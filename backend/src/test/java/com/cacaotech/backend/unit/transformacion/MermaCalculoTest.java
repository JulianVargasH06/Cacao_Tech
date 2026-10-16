package com.cacaotech.backend.unit.transformacion;

import com.cacaotech.backend.transformacion.MermaCalculadora;
import com.cacaotech.backend.transformacion.ResultadoMerma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias para {@link MermaCalculadora}.
 *
 * Sin anotaciones Spring — no levanta contexto, no conecta a BD.
 * Cubre: cálculos válidos y todas las reglas de validación de entrada.
 */
@DisplayName("MermaCalculadora — pruebas unitarias")
class MermaCalculoTest {

    private MermaCalculadora calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new MermaCalculadora();
    }

    // ─── Casos de cálculo correctos ───────────────────────────────────────────

    @Test
    @DisplayName("MR-01 — Caso de referencia: 100 kg entrada, 80 kg salida → 20 kg / 20.00 %")
    void debeCumplirCasoDeReferencia() {
        ResultadoMerma resultado = calculadora.calcular(
                new BigDecimal("100"), new BigDecimal("80"));

        assertThat(resultado.mermaKg())
                .as("merma absoluta en kg")
                .isEqualByComparingTo("20");
        assertThat(resultado.mermaPorcentaje())
                .as("merma porcentual")
                .isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("MR-02 — 100 kg entrada, 85 kg salida → 15 kg / 15.00 %")
    void debeCalcularMermaDeQuincePorCiento() {
        ResultadoMerma resultado = calculadora.calcular(
                new BigDecimal("100"), new BigDecimal("85"));

        assertThat(resultado.mermaKg())
                .as("merma absoluta en kg")
                .isEqualByComparingTo("15");
        assertThat(resultado.mermaPorcentaje())
                .as("merma porcentual")
                .isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("MR-03 — Entrada igual a salida (100 kg = 100 kg) → merma 0 kg / 0.00 %")
    void debeRetornarMermaCeroConEntradaIgualASalida() {
        ResultadoMerma resultado = calculadora.calcular(
                new BigDecimal("100"), new BigDecimal("100"));

        assertThat(resultado.mermaKg())
                .as("merma absoluta en kg")
                .isEqualByComparingTo("0");
        assertThat(resultado.mermaPorcentaje())
                .as("merma porcentual")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Salida cero — 100 kg entrada, 0 kg salida → merma 100 kg / 100.00 %")
    void debeCalcularMermaTotalConSalidaCero() {
        ResultadoMerma resultado = calculadora.calcular(
                new BigDecimal("100"), BigDecimal.ZERO);

        assertThat(resultado.mermaKg())
                .as("merma absoluta en kg")
                .isEqualByComparingTo("100");
        assertThat(resultado.mermaPorcentaje())
                .as("merma porcentual")
                .isEqualByComparingTo("100.00");
    }

    // ─── Casos de entradas inválidas ──────────────────────────────────────────

    @Test
    @DisplayName("MR-05 — Entrada cero → IllegalArgumentException con mención a 'cero'")
    void debeLanzarExcepcionConEntradaCero() {
        assertThatThrownBy(() ->
                calculadora.calcular(BigDecimal.ZERO, new BigDecimal("80")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cero");
    }

    @Test
    @DisplayName("Entrada negativa → IllegalArgumentException con mención a 'negativa'")
    void debeLanzarExcepcionConEntradaNegativa() {
        assertThatThrownBy(() ->
                calculadora.calcular(new BigDecimal("-1"), new BigDecimal("80")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    @DisplayName("Salida negativa → IllegalArgumentException con mención a 'negativa'")
    void debeLanzarExcepcionConSalidaNegativa() {
        assertThatThrownBy(() ->
                calculadora.calcular(new BigDecimal("100"), new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativa");
    }

    @Test
    @DisplayName("MR-04 — Salida superior a entrada (80 kg → 100 kg) → IllegalArgumentException")
    void debeLanzarExcepcionCuandoSalidaSuperaEntrada() {
        assertThatThrownBy(() ->
                calculadora.calcular(new BigDecimal("80"), new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("superar");
    }
}
