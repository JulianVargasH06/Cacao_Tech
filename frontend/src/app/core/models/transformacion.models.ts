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
    costoAsignadoCop?: number; // solo en la respuesta; null si la transformación aún no tiene costos
    costoUnitarioCop?: number;
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
    costoTotalCop?: number; // suma de sus costos (HU05)
    alerta?: { id: number; tipo: string; descripcion: string } | null; // merma alta: los lotes resultantes quedaron bloqueados
}

/** Cuerpo de POST /api/transformaciones/{id}/costos (HU05). */
export interface CostoRequest {
    categoria: string; // ej. MANO_DE_OBRA, ENERGIA, EMPAQUE
    descripcion: string;
    valorCop: number;
}

/** Costos de una transformación y su reparto entre las salidas según sus kg. */
export interface CostosTransformacion {
    transformacionId: number;
    codigo: string;
    costoTotalCop: number;
    costos: { id: number; categoria: string; descripcion: string; valorCop: number; fecha: string; usuario: string }[];
    salidas: { loteId: number; codigo: string; cantidadKg: number; costoAsignadoCop?: number; costoUnitarioCop?: number }[];
}
