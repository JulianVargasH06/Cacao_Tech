export type EstadoProducto = 'LIBERADO' | 'BLOQUEADO';

export interface CambioEstadoRequest {
    estado: EstadoProducto;
    motivo: string; // obligatorio
}

export interface FichaPublica {
    codigo: string;
    nombre?: string;
    origen: string;
    estado: EstadoProducto | string;
    recorrido: { fecha: string; etapa: string; descripcion?: string }[];
}