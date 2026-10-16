package com.cacaotech.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ficha pública del producto (HU06): lo que ve un comprador sin iniciar sesión.
 * NO incluye información privada: usuarios, cantidades internas, costos, incidencias ni motivos de bloqueo.
 *
 * codigo, origen, estado y recorrido coinciden con el modelo FichaPublica del frontend.
 */
public record FichaPublicaDto(
        String codigo,
        String estado, // PENDIENTE, LIBERADO o BLOQUEADO
        String estadoDescripcion, // texto para el comprador (ej. "Liberado para la venta")
        String tipo, // "Materia prima recibida" o "Producto transformado"
        LocalDate fechaRegistro,
        String productor, // productores de origen, separados por coma
        String origen, // lugares de origen, separados por coma
        List<Origen> origenes,
        List<Paso> recorrido // de la finca al producto, en orden cronológico
) {

    /** Un lote recibido de un productor: el punto de partida del recorrido. */
    public record Origen(String codigoLote, String productor, String origen, LocalDate fechaRecepcion) {}

    /** Una etapa del recorrido: recepción o una transformación. */
    public record Paso(LocalDateTime fecha, String etapa, String descripcion) {}
}
