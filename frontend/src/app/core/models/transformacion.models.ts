export interface TransformacionRequest {
    loteId: number;
    etapa: string;
    entradas: number;
    salidas: number;
    responsable: string;
    fecha: string; // formato yyyy-MM-dd
    incidencias?: string;
}

export interface Transformacion extends TransformacionRequest {
    id: number;
}