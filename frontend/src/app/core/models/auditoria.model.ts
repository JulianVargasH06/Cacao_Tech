// Sigue AuditoriaResponseDto del backend (GET /api/auditoria).

/** Un cambio registrado por el sistema (HU10): quién hizo qué, sobre qué y cómo quedó. */
export interface RegistroAuditoria {
    id: number;
    usuario: string; // nombre de quien hizo el cambio ("sistema" si no hubo usuario)
    correoUsuario?: string;
    entidad: string; // ej. "Lote", "Transformacion"
    entidadId: number;
    accion: string; // "CREAR", "CONSUMO", "CAMBIAR_ESTADO"...
    valoresAnteriores?: string; // JSON en texto
    valoresNuevos?: string; // JSON en texto
    fecha: string; // formato yyyy-MM-ddTHH:mm:ss
}
