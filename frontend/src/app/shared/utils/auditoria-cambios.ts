import { RegistroAuditoria } from '../../core/models/auditoria.model';

/** Un campo que cambió: "cantidadDisponible: 100 → 60". antes es undefined cuando el campo es nuevo (CREAR). */
export interface CambioCampo {
    campo: string;
    antes?: string;
    despues?: string;
}

/**
 * Convierte los valores de antes y después de un registro de auditoría (JSON en texto)
 * en una lista de campos legibles. Si el texto no es JSON, se muestra tal cual.
 */
export function cambiosDeAuditoria(registro: RegistroAuditoria): CambioCampo[] {
    const antes = leerJson(registro.valoresAnteriores);
    const despues = leerJson(registro.valoresNuevos);

    if (!antes && !despues) {
        const texto = registro.valoresNuevos ?? registro.valoresAnteriores;
        return texto ? [{ campo: '', despues: texto }] : [];
    }

    const campos = [...new Set([...Object.keys(antes ?? {}), ...Object.keys(despues ?? {})])];
    return campos.map((campo) => ({
        campo,
        antes: antes && campo in antes ? legible(antes[campo]) : undefined,
        despues: despues && campo in despues ? legible(despues[campo]) : undefined
    }));
}

function leerJson(texto?: string | null): Record<string, unknown> | null {
    if (!texto) return null;
    try {
        const valor = JSON.parse(texto);
        return valor && typeof valor === 'object' && !Array.isArray(valor) ? (valor as Record<string, unknown>) : null;
    } catch {
        return null; // texto plano, no JSON
    }
}

/** Valor para mostrar: números con formato colombiano y las listas de entradas/salidas como "L-001 (40 kg)". */
function legible(valor: unknown): string {
    if (valor === null || valor === undefined || valor === '') return '—';
    if (typeof valor === 'number') return valor.toLocaleString('es-CO', { maximumFractionDigits: 3 });
    if (Array.isArray(valor)) return valor.map(legible).join(', ');
    if (typeof valor === 'object') {
        const objeto = valor as Record<string, unknown>;
        if ('lote' in objeto && 'cantidadKg' in objeto) return `${objeto['lote']} (${legible(objeto['cantidadKg'])} kg)`;
        return JSON.stringify(objeto);
    }
    return String(valor);
}
