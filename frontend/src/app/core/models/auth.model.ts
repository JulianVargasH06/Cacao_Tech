// OJO: nombres de campos propuestos. Confirmar con el equipo de backend.

export type Rol = 'OPERADOR' | 'ADMINISTRADOR';

export interface LoginRequest {
    usuario: string;
    contrasena: string;
}

export interface LoginResponse {
    token: string;
    rol: string; // lo que devuelva el backend; se normaliza con normalizarRol()
    nombre?: string;
}

export interface Sesion {
    token: string;
    rol: Rol;
    nombre: string;
}

/** Convierte lo que mande el backend ("ROLE_ADMIN", "admin", "Operador"...) a nuestro tipo Rol. */
export function normalizarRol(valor: string): Rol {
    const limpio = valor.toUpperCase().replace('ROLE_', '');
    return limpio.startsWith('ADMIN') ? 'ADMINISTRADOR' : 'OPERADOR';
}