import { HttpErrorResponse } from '@angular/common/http';

/** Forma de los errores del backend (GlobalExceptionHandler): { error, detalles? }. */
export interface ErrorApi {
    error?: string;
    detalles?: Record<string, string>; // solo en errores de validación: campo → mensaje
}

/** Convierte una respuesta de error del backend en un mensaje para el usuario. */
export function mensajeDeError(err: HttpErrorResponse, porDefecto = 'Intenta de nuevo en unos segundos'): string {
    // 0 = no hubo respuesta; 5xx = falla del servidor (el proxy de ng serve responde 500 si el backend está apagado)
    if (err.status === 0 || err.status >= 500) return 'El servidor no respondió. ¿Está corriendo el backend?';
    if (err.status === 403) return 'No tienes permiso para hacer esta operación';

    const cuerpo = err.error as ErrorApi | null;
    // En un error de validación el mensaje útil está en "detalles" (ej. "La fecha no puede ser futura")
    const detalle = cuerpo?.detalles ? Object.values(cuerpo.detalles)[0] : undefined;
    return detalle ?? cuerpo?.error ?? porDefecto;
}

/** true si el backend rechazó la operación porque ya existe un lote con ese código. */
export function esCodigoDuplicado(err: HttpErrorResponse): boolean {
    const mensaje = (err.error as ErrorApi | null)?.error ?? '';
    return err.status === 400 && /ya existe un lote/i.test(mensaje);
}
