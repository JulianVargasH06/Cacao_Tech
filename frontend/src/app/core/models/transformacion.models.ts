// OJO: forma tomada de las entidades Transformacion, EntradaTransformacion y SalidaTransformacion
// del backend (PR #7). Confirmar cuando exista el JSON real de POST /api/transformaciones.

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
}

/** Lo que sale del proceso: crea un lote nuevo con este código. */
export interface SalidaTransformacion {
    codigo: string;
    cantidadKg: number;
}

/** Lo que enviamos al registrar una transformación (HU02). El responsable sale del token. */
export interface TransformacionRequest {
    tipo: Etapa;
    fecha: string; // formato yyyy-MM-ddTHH:mm:ss (hora local)
    entradas: EntradaTransformacion[];
    salidas: SalidaTransformacion[];
    observaciones?: string;
}

/** Lo que devuelve el backend: lo enviado + id, código generado y responsable. */
export interface Transformacion extends TransformacionRequest {
    id: number;
    codigo: string;
    responsable: string;
}
