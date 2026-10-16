/** 1234.5 → "1.234,5" (formato colombiano). */
export function formatoKg(valor: number | null | undefined): string {
    if (valor === null || valor === undefined) return '—';
    return valor.toLocaleString('es-CO', { maximumFractionDigits: 2 });
}

/** 1234567.8 → "$ 1.234.568" (pesos colombianos, sin centavos). */
export function formatoCop(valor: number | null | undefined): string {
    if (valor === null || valor === undefined) return '—';
    return '$ ' + Math.round(valor).toLocaleString('es-CO');
}

/** 26.666 → "26,7 %" */
export function formatoPorcentaje(valor: number | null | undefined, decimales = 1): string {
    if (valor === null || valor === undefined) return '—';
    return valor.toLocaleString('es-CO', { minimumFractionDigits: decimales, maximumFractionDigits: decimales }) + ' %';
}

/** Nombre para mostrar de un tipo de alerta del backend. */
export function etiquetaAlerta(tipo: string): string {
    const nombres: Record<string, string> = { MERMA_ALTA: 'Merma alta', INCIDENCIA: 'Incidencia', RETIRO: 'Retiro' };
    return nombres[tipo] ?? tipo;
}

/** Severidad del tag de PrimeNG según el estado de un lote. */
export function severidadEstado(estado: string): 'success' | 'warn' | 'danger' | 'secondary' {
    if (estado === 'LIBERADO') return 'success';
    if (estado === 'BLOQUEADO') return 'danger';
    return 'warn'; // PENDIENTE
}
