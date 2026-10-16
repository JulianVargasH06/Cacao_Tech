// Sigue TransformacionRequestDto y TransformacionResponseDto del backend (POST y GET /api/transformaciones).

export type Etapa = 'FERMENTACION' | 'SECADO' | 'TOSTADO' | 'MOLIENDA' | 'EMPAQUE';

export const ETAPAS: { valor: Etapa; etiqueta: string }[] = [
    { valor: 'FERMENTACION', etiqueta: 'Fermentación' },
    { valor: 'SECADO', etiqueta: 'Secado' },
    { valor: 'TOSTADO', etiqueta: 'Tostado' },
    { valor: 'MOLIENDA', etiqueta: 'Molienda' },
    { valor: 'EMPAQUE', etiqueta: 'Empaque' }
];

/** Cacao que se toma de un lote existente (se descuenta de su disponible). */
export interface EntradaTransformacion {
    loteId: number;
    cantidadKg: number;
    codigoLote?: string; // solo en la respuesta
}

/** Lo que sale del proceso: crea un lote nuevo con este código. */
export interface SalidaTransformacion {
    codigo: string;
    cantidadKg: number;
    loteId?: number; // solo en la respuesta: el lote nuevo que se creó
}

/** Lo que enviamos al registrar una transformación (HU02). El responsable sale del token. */
export interface TransformacionRequest {
    tipo: Etapa;
    fecha: string; // formato yyyy-MM-ddTHH:mm:ss (hora local)
    entradas: EntradaTransformacion[];
    salidas: SalidaTransformacion[];
    observaciones?: string;
}

/** Lo que devuelve el backend: lo enviado + id, código generado, responsable y merma. */
export interface Transformacion extends TransformacionRequest {
    id: number;
    codigo: string; // ej. "TR-00001"
    responsable: string; // nombre del usuario que la registró
    mermaKg?: number;
    mermaPorcentaje?: number;
}
