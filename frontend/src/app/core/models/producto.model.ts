// Ficha pública (HU06) y cambio de estado de un lote/producto (HU09). Siguen los DTOs del backend.

/** Cuerpo de PATCH /api/lotes/{id}/estado: el motivo es obligatorio. */
export interface CambioEstadoRequest {
    estado: 'LIBERADO' | 'BLOQUEADO';
    motivo: string;
}

/** Un cambio de estado de un lote (GET /api/lotes/{id}/historial-estados). */
export interface HistorialEstado {
    id: number;
    estadoAnterior?: string;
    estadoNuevo: string;
    motivo: string;
    usuario: string;
    fecha: string;
}

/** GET /api/publico/productos/{codigo}: lo que ve el comprador, sin información privada. */
export interface FichaPublica {
    codigo: string;
    estado: string; // PENDIENTE, LIBERADO o BLOQUEADO
    estadoDescripcion: string; // ej. "Liberado para la venta"
    tipo: string; // "Materia prima recibida" o "Producto transformado"
    fechaRegistro: string;
    productor: string;
    origen: string;
    origenes: { codigoLote: string; productor?: string; origen?: string; fechaRecepcion: string }[];
    recorrido: { fecha: string; etapa: string; descripcion: string }[];
}
