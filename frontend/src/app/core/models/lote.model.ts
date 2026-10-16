// OJO: nombres tomados de model/Lote.java del backend (PR #7). Confirmar cuando exista el JSON real de la API.

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

export interface EventoTrazabilidad {
    fecha: string;
    etapa: string;
    descripcion: string;
    cantidad?: number;
    unidad?: string;
}

export interface TrazabilidadLote {
    lote: Lote;
    eventos: EventoTrazabilidad[];
}