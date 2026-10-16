// Sigue IncidenciaRequestDto e IncidenciaResponseDto del backend (/api/incidencias).

export interface IncidenciaRequest {
    transformacionId: number;
    loteId?: number;
    tipo: string;
    descripcion: string;
    bloquearLote: boolean; // true: bloquea el lote y genera una alerta
}

export interface Incidencia {
    id: number;
    transformacionId: number;
    codigoTransformacion: string;
    loteId?: number;
    codigoLote?: string;
    tipo: string;
    descripcion: string;
    fecha: string;
    usuario: string;
    alertaId?: number;
}
