package com.cacaotech.backend.transformacion;

import java.math.BigDecimal;

/**
 * Representación inmutable de un lote o producto de salida en una transformación.
 * Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * @param id Identificador textual único del lote de salida (no nulo ni en blanco).
 * @param kg Cantidad en kilogramos producida; debe ser estrictamente positiva (> 0).
 */
public record LoteSalida(String id, BigDecimal kg) {
}
