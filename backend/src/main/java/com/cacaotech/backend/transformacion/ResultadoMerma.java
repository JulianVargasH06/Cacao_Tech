package com.cacaotech.backend.transformacion;

import java.math.BigDecimal;

/**
 * Resultado inmutable del cálculo de merma para una transformación.
 * Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * @param mermaKg          Merma absoluta en kilogramos (entrada - salida).
 * @param mermaPorcentaje  Merma relativa expresada como porcentaje
 *                         ((entrada - salida) / entrada × 100),
 *                         redondeada a 2 decimales con HALF_UP.
 */
public record ResultadoMerma(BigDecimal mermaKg, BigDecimal mermaPorcentaje) {
}
