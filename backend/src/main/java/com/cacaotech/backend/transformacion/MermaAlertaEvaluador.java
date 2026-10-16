package com.cacaotech.backend.transformacion;

import java.math.BigDecimal;

/**
 * Evaluador de alertas por umbral de merma.
 * Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * <p>Regla de negocio:
 * <ul>
 *   <li>Se genera alerta cuando la merma es <strong>estrictamente mayor</strong> al 15 %.</li>
 *   <li>Una merma exactamente igual al 15 % <strong>no</strong> genera alerta.</li>
 * </ul>
 *
 * <p>La comparación se realiza con {@link BigDecimal#compareTo} para evitar
 * errores de precisión derivados de diferencias de escala o aritmética de punto flotante.
 *
 * <p>Esta clase es independiente de {@link MermaCalculadora}: recibe directamente
 * el porcentaje ya calculado y lo evalúa frente al umbral.
 */
public class MermaAlertaEvaluador {

    /**
     * Umbral de alerta: merma ESTRICTAMENTE superior al 15 %.
     * Declarado como constante para facilitar futuros ajustes de negocio.
     */
    public static final BigDecimal UMBRAL = new BigDecimal("15"); // público: las alertas lo citan en su descripción

    /**
     * Determina si un porcentaje de merma supera el umbral de alerta.
     *
     * @param mermaPorcentaje porcentaje de merma a evaluar; debe ser no nulo y no negativo
     * @return {@code true} si {@code mermaPorcentaje > 15}, {@code false} en caso contrario
     * @throws IllegalArgumentException si {@code mermaPorcentaje} es {@code null} o negativo
     */
    public boolean superaUmbral(BigDecimal mermaPorcentaje) {
        validar(mermaPorcentaje);
        return mermaPorcentaje.compareTo(UMBRAL) > 0;
    }

    // ─── Validación ───────────────────────────────────────────────────────────

    private void validar(BigDecimal mermaPorcentaje) {
        if (mermaPorcentaje == null) {
            throw new IllegalArgumentException(
                    "El porcentaje de merma no puede ser nulo.");
        }
        if (mermaPorcentaje.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El porcentaje de merma no puede ser negativo. Valor recibido: "
                    + mermaPorcentaje);
        }
    }
}
