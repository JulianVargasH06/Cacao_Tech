// Sigue model/Lote.java y los DTOs de /api/lotes del backend.

export type EstadoLote = 'PENDIENTE' | 'LIBERADO' | 'BLOQUEADO';
export type TipoLote = 'RECEPCION' | 'RESULTADO_TRANSFORMACION';

/** Lo que enviamos al registrar un lote (HU01). La unidad no se envía: el backend solo maneja KG. */
export interface LoteRequest {
    codigo: string;
    productor: string;
    origen: string;
    fechaRegistro: string; // formato yyyy-MM-dd
    cantidadInicial: number; // en kg
}

/** Lo que devuelve el backend: lo enviado + datos que asigna él. */
export interface Lote extends LoteRequest {
    id: number;
    cantidadDisponible: number;
    unidad: string; // siempre "KG"
    tipoLote: TipoLote; // RECEPCION si llegó de un productor, RESULTADO_TRANSFORMACION si salió de un proceso
    estado: EstadoLote; // un lote BLOQUEADO no se puede transformar
}

/** Filtros opcionales de GET /api/lotes y GET /api/lotes/exportar (HU11). */
export interface FiltroLotes {
    codigo?: string; // contiene, sin importar mayúsculas
    estado?: EstadoLote;
    tipoLote?: TipoLote;
    desde?: string; // yyyy-MM-dd
    hasta?: string; // yyyy-MM-dd
}

/** GET /api/lotes/{id}/margen (HU05): margen sobre venta de un lote resultante. */
export interface Margen {
    loteId: number;
    codigo: string;
    costoUnitarioCop: number;
    precioVentaKgCop: number;
    gananciaPorKgCop: number; // negativa si se vende por debajo del costo
    margenPorcentaje: number;
}