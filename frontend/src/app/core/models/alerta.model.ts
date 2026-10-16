// Sigue AlertaResponseDto del backend (GET /api/alertas).

export type EstadoAlerta = 'ACTIVA' | 'RESUELTA';

/** Alerta explicable (HU04): por merma alta en una transformación o por una incidencia que bloqueó un lote. */
export interface Alerta {
    id: number;
    tipo: string; // MERMA_ALTA o INCIDENCIA
    severidad: string;
    descripcion: string;
    loteId?: number;
    codigoLote?: string;
    transformacionId?: number;
    codigoTransformacion?: string;
    estado: EstadoAlerta;
    creadaEn: string;
    resueltaEn?: string;
    resueltaPor?: string;
}
