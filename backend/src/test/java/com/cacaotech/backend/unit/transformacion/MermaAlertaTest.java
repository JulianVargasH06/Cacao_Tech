package com.cacaotech.backend.unit.transformacion;

import com.cacaotech.backend.transformacion.MermaAlertaEvaluador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias para {@link MermaAlertaEvaluador}.
 *
 * Sin anotaciones Spring — no levanta contexto, no conecta a BD.
 * Cubre: umbral exacto, valores límite, caso de referencia y entradas inválidas.
 */
@DisplayName("MermaAlertaEvaluador — pruebas unitarias")
class MermaAlertaTest {

    private MermaAlertaEvaluador evaluador;

    @BeforeEach
    void setUp() {
        evaluador = new MermaAlertaEvaluador();
    }

    // ─── Sin alerta (merma ≤ 15 %) ────────────────────────────────────────────

    @Test
    @DisplayName("AL-04 — Merma 0 % → no genera alerta")
    void noDebeGenerarAlertaConMermaCero() {
        assertThat(evaluador.superaUmbral(new BigDecimal("0.00")))
                .as("merma 0 % no debe superar el umbral")
                .isFalse();
    }

    @Test
    @DisplayName("AL-03 — Merma 14.99 % (por debajo del umbral) → no genera alerta")
    void noDebeGenerarAlertaConMermaPorDebajoDelUmbral() {
        assertThat(evaluador.superaUmbral(new BigDecimal("14.99")))
                .as("merma 14.99 % no debe superar el umbral")
                .isFalse();
    }

    @Test
    @DisplayName("AL-02 — Merma exactamente 15.00 % (escala 2) → no genera alerta")
    void noDebeGenerarAlertaConMermaExactaDeUmbralConEscalaDos() {
        assertThat(evaluador.superaUmbral(new BigDecimal("15.00")))
                .as("merma 15.00 % es igual al umbral y no debe superarlo")
                .isFalse();
    }

    @Test
    @DisplayName("AL-02b — Merma exactamente 15 % (sin decimales, distinta escala) → no genera alerta")
    void noDebeGenerarAlertaConMermaExactaDeUmbralSinDecimales() {
        // Verifica que compareTo es inmune a diferencias de escala (15 vs 15.00)
        assertThat(evaluador.superaUmbral(new BigDecimal("15")))
                .as("merma 15 (sin escala) es igual al umbral y no debe superarlo")
                .isFalse();
    }

    // ─── Con alerta (merma > 15 %) ────────────────────────────────────────────

    @Test
    @DisplayName("— Merma 15.01 % (mínimo que supera el umbral) → genera alerta")
    void debeGenerarAlertaConMermaMinimaSobreUmbral() {
        assertThat(evaluador.superaUmbral(new BigDecimal("15.01")))
                .as("merma 15.01 % debe superar el umbral")
                .isTrue();
    }

    @Test
    @DisplayName("AL-01 — Caso de referencia: merma 20 % → genera alerta")
    void debeGenerarAlertaConElCasoDeReferencia() {
        assertThat(evaluador.superaUmbral(new BigDecimal("20.00")))
                .as("merma 20 % (caso de referencia 100 kg → 80 kg) debe superar el umbral")
                .isTrue();
    }

    // ─── Entradas inválidas ───────────────────────────────────────────────────

    @Test
    @DisplayName("— Porcentaje nulo → IllegalArgumentException")
    void debeLanzarExcepcionConPorcentajeNulo() {
        assertThatThrownBy(() -> evaluador.superaUmbral(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulo");
    }

    @Test
    @DisplayName("— Porcentaje negativo → IllegalArgumentException")
    void debeLanzarExcepcionConPorcentajeNegativo() {
        assertThatThrownBy(() -> evaluador.superaUmbral(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }
}
