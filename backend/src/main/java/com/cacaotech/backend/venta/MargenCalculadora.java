package com.cacaotech.backend.venta;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculadora de margen porcentual sobre la venta para productos y lotes de cacao.
 * Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * <p>Fórmula aplicada:
 * <pre>
 *   Margen (%) = ((precioVentaKg - costoUnitarioKg) / precioVentaKg) × 100
 * </pre>
 *
 * <p>Reglas de negocio y validación:
 * <ul>
 *   <li>El precio de venta y el costo unitario no pueden ser nulos.</li>
 *   <li>El precio de venta debe ser estrictamente positivo (&gt; 0).</li>
 *   <li>El costo unitario debe ser mayor o igual a cero (&gt;= 0).</li>
 *   <li>Si el costo unitario supera el precio de venta, se permite el cálculo y genera un margen negativo.</li>
 *   <li>El resultado final se redondea a 2 decimales usando {@link RoundingMode#HALF_UP}.</li>
 * </ul>
 */
public class MargenCalculadora {

    private static final int ESCALA_FINAL = 2;
    private static final int ESCALA_INTERMEDIA = 10;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /**
     * Calcula el margen porcentual sobre el precio de venta por kilogramo.
     *
     * @param precioVentaKg   precio de venta o hipotético por kg; debe ser estrictamente &gt; 0.
     * @param costoUnitarioKg costo unitario por kg; debe ser no negativo (&gt;= 0).
     * @return margen porcentual con escala de 2 decimales y redondeo HALF_UP.
     * @throws IllegalArgumentException si algún parámetro es nulo o viola las reglas de validación.
     */
    public BigDecimal calcularMargen(BigDecimal precioVentaKg, BigDecimal costoUnitarioKg) {
        validar(precioVentaKg, costoUnitarioKg);

        BigDecimal gananciaUnitario = precioVentaKg.subtract(costoUnitarioKg);

        return gananciaUnitario
                .divide(precioVentaKg, ESCALA_INTERMEDIA, REDONDEO)
                .multiply(BigDecimal.valueOf(100))
                .setScale(ESCALA_FINAL, REDONDEO);
    }

    private void validar(BigDecimal precioVentaKg, BigDecimal costoUnitarioKg) {
        if (precioVentaKg == null) {
            throw new IllegalArgumentException("El precio de venta no puede ser nulo.");
        }
        if (costoUnitarioKg == null) {
            throw new IllegalArgumentException("El costo unitario no puede ser nulo.");
        }
        if (precioVentaKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El precio de venta debe ser estrictamente positivo (> 0). Valor recibido: " + precioVentaKg);
        }
        if (costoUnitarioKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El costo unitario no puede ser negativo. Valor recibido: " + costoUnitarioKg);
        }
    }
}
