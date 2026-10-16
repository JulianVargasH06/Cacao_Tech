import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Hoy a las 23:59:59. Sirve como tope de calendarios y validadores. */
export function finDelDia(): Date {
    const fecha = new Date();
    fecha.setHours(23, 59, 59, 999);
    return fecha;
}

/** Validador: la fecha no puede ser posterior a hoy. */
export function fechaNoFutura(control: AbstractControl): ValidationErrors | null {
    const fecha = control.value as Date | null;
    if (!fecha) return null; // si está vacía, de eso se encarga Validators.required
    return fecha > finDelDia() ? { fechaFutura: true } : null;
}

/**
 * Validador para fecha + hora: no puede ser posterior a este momento
 * (el backend aplica la misma regla con @PastOrPresent).
 */
export function fechaHoraNoFutura(control: AbstractControl): ValidationErrors | null {
    const fecha = control.value as Date | null;
    if (!fecha) return null;
    return fecha.getTime() > Date.now() ? { fechaFutura: true } : null;
}

/** Convierte un Date a 'yyyy-MM-dd' usando la hora local (no UTC). */
export function aFechaIso(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
}

/** Convierte un Date a 'yyyy-MM-ddTHH:mm:ss' en hora local (lo que espera un LocalDateTime de Java). */
export function aFechaHoraIso(fecha: Date): string {
    const hora = String(fecha.getHours()).padStart(2, '0');
    const minutos = String(fecha.getMinutes()).padStart(2, '0');
    return `${aFechaIso(fecha)}T${hora}:${minutos}:00`;
}
