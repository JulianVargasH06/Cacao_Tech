package com.cacaotech.backend.transformacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de lógica pura para distribuir proporcionalmente el costo total
 * de una transformación entre sus lotes de salida en función de los kilogramos obtenidos.
 *
 * <p>Clase POJO pura: no depende de JPA, Spring ni base de datos.
 *
 * <p>Garantía de conservación de valor:
 * Aplica redondeo {@link RoundingMode#HALF_UP} a dos decimales y ajusta el residuo
 * en la última asignación para asegurar que {@code suma(asignaciones) == costoTotal} exactamente.
 */
public class CostoDistribucionService {

    private static final int ESCALA_MONEDA = 2;
    private static final int ESCALA_INTERMEDIA = 10;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /**
     * Distribuye el costo total entre los lotes de salida de forma proporcional a su peso en kg.
     *
     * @param costoTotal importe total acumulado en la transformación; no nulo y >= 0.
     * @param salidas    lista no vacía de lotes resultantes; cada uno con kg > 0 e id no vacío.
     * @return lista de costos asignados en el mismo orden que {@code salidas}, con escala 2.
     * @throws IllegalArgumentException si algún argumento o elemento viola las reglas de negocio.
     */
    public List<BigDecimal> distribuir(BigDecimal costoTotal, List<LoteSalida> salidas) {
        validarEntradas(costoTotal, salidas);

        int n = salidas.size();
        List<BigDecimal> resultado = new ArrayList<>(n);

        // Si solo hay una salida, recibe el 100% del costo total con escala 2
        if (n == 1) {
            resultado.add(costoTotal.setScale(ESCALA_MONEDA, REDONDEO));
            return resultado;
        }

        // Suma total de kilogramos producidos
        BigDecimal kgTotal = BigDecimal.ZERO;
        for (LoteSalida salida : salidas) {
            kgTotal = kgTotal.add(salida.kg());
        }

        BigDecimal sumaAsignada = BigDecimal.ZERO;

        // Asignar a los primeros n - 1 lotes proporcionalmente con HALF_UP
        for (int i = 0; i < n - 1; i++) {
            BigDecimal kgActual = salidas.get(i).kg();
            BigDecimal proporcion = kgActual.divide(kgTotal, ESCALA_INTERMEDIA, REDONDEO);
            BigDecimal asignacion = costoTotal.multiply(proporcion).setScale(ESCALA_MONEDA, REDONDEO);

            resultado.add(asignacion);
            sumaAsignada = sumaAsignada.add(asignacion);
        }

        // El último elemento absorbe la diferencia exacta (last-cent adjustment)
        BigDecimal costoTotalEscala = costoTotal.setScale(ESCALA_MONEDA, REDONDEO);
        BigDecimal ultimaAsignacion = costoTotalEscala.subtract(sumaAsignada);
        resultado.add(ultimaAsignacion);

        return resultado;
    }

    private void validarEntradas(BigDecimal costoTotal, List<LoteSalida> salidas) {
        if (costoTotal == null) {
            throw new IllegalArgumentException("El costo total no puede ser nulo.");
        }
        if (costoTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El costo total no puede ser negativo. Valor recibido: " + costoTotal);
        }
        if (salidas == null) {
            throw new IllegalArgumentException("La lista de salidas no puede ser nula.");
        }
        if (salidas.isEmpty()) {
            throw new IllegalArgumentException("La lista de salidas no puede estar vacía.");
        }

        for (int i = 0; i < salidas.size(); i++) {
            LoteSalida lote = salidas.get(i);
            if (lote == null) {
                throw new IllegalArgumentException("El lote de salida en el índice " + i + " no puede ser nulo.");
            }
            if (lote.id() == null || lote.id().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "El identificador del lote en el índice " + i + " no puede ser nulo ni vacío.");
            }
            if (lote.kg() == null) {
                throw new IllegalArgumentException(
                        "Los kilogramos del lote " + lote.id() + " no pueden ser nulos.");
            }
            if (lote.kg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Los kilogramos del lote " + lote.id() + " deben ser estrictamente positivos (> 0). Valor recibido: " + lote.kg());
            }
        }
    }
}
