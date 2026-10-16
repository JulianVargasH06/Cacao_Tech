package com.cacaotech.backend.transformacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculadora de merma para transformaciones de cacao.
 * Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * <p>Fórmulas aplicadas:
 * <pre>
 *   mermaKg         = entrada - salida
 *   mermaPorcentaje = (entrada - salida) / entrada × 100
 * </pre>
 *
 * <p>Reglas de validación:
 * <ul>
 *   <li>entrada y salida no pueden ser {@code null}.</li>
 *   <li>entrada debe ser estrictamente positiva (> 0).</li>
 *   <li>salida debe ser no negativa (>= 0).</li>
 *   <li>salida no puede superar a entrada.</li>
 * </ul>
 */
public class MermaCalculadora {

    /** Escala final del porcentaje de merma (2 decimales). */
    private static final int ESCALA_PORCENTAJE = 2;

    /**
     * Escala intermedia usada durante la división para preservar precisión
     * antes del redondeo final.
     */
    private static final int ESCALA_INTERMEDIA = 10;

    /** Política de redondeo aplicada en todo el cálculo. */
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /**
     * Calcula la merma absoluta y porcentual de una transformación.
     *
     * @param entrada kg de materia prima que entran al proceso; debe ser &gt; 0
     * @param salida  kg de producto obtenido; debe ser &gt;= 0 y &lt;= entrada
     * @return {@link ResultadoMerma} con {@code mermaKg} y {@code mermaPorcentaje}
     * @throws IllegalArgumentException si algún parámetro viola las reglas de validación
     */
    public ResultadoMerma calcular(BigDecimal entrada, BigDecimal salida) {
        validar(entrada, salida);

        BigDecimal mermaKg = entrada.subtract(salida);

        BigDecimal mermaPorcentaje = mermaKg
                .divide(entrada, ESCALA_INTERMEDIA, REDONDEO)
                .multiply(BigDecimal.valueOf(100))
                .setScale(ESCALA_PORCENTAJE, REDONDEO);

        return new ResultadoMerma(mermaKg, mermaPorcentaje);
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validar(BigDecimal entrada, BigDecimal salida) {
        if (entrada == null || salida == null) {
            throw new IllegalArgumentException(
                    "Los valores de entrada y salida no pueden ser nulos.");
        }
        if (entrada.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "La entrada no puede ser negativa. Valor recibido: " + entrada);
        }
        if (salida.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "La salida no puede ser negativa. Valor recibido: " + salida);
        }
        if (entrada.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "La entrada no puede ser cero (se produciría división por cero).");
        }
        if (salida.compareTo(entrada) > 0) {
            throw new IllegalArgumentException(
                    "La salida (" + salida + " kg) no puede superar a la entrada (" + entrada + " kg).");
        }
    }
}
