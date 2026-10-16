// OJO: forma tomada de model/Auditoria.java del backend (PR #7). Confirmar cuando exista GET /api/auditoria.

/** Un cambio registrado por el sistema (HU10): quién hizo qué, sobre qué y cómo quedó. */
export interface RegistroAuditoria {
    id: number;
    usuario: string;
    entidad: string; // ej. "Lote", "Transformacion"
    entidadId: number;
    accion: string; // ej. "CREAR", "CAMBIAR_ESTADO"
    valoresAnteriores?: string;
    valoresNuevos?: string;
    fecha: string; // formato yyyy-MM-ddTHH:mm:ss
}
