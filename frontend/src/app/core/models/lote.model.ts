export interface LoteRequest {
    productor: string;
    origen: string;
    fecha: string; // formato yyyy-MM-dd
    cantidad: number;
    unidad: string;
    identificador: string;
}

export interface Lote {
    id: number;
    identificador: string;
    productor: string;
    origen: string;
    fecha: string;
    unidad: string;
    cantidadRecibida: number;
    cantidadConsumida: number;
    cantidadDisponible: number;
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