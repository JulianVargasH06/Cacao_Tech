export type Unidad = 'kg' | 'arroba' | 'bulto';
export const UNIDADES: Unidad[] = ['kg', 'arroba', 'bulto'];
export interface LoteRequest {
    codigo: string;
    productor: string;
    origen: string;
    fechaRecepcion: string; // formato yyyy-MM-dd
    cantidadRecibida: number;
    unidad: Unidad;
}

export interface Lote extends LoteRequest {
    id: number;
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