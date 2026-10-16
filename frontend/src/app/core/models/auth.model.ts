// Tomado de LoginRequestDto y AuthResponseDto del backend (POST /api/auth/login).

export type Rol = 'OPERADOR' | 'ADMINISTRADOR';

export interface LoginRequest {
    correo: string;
    password: string;
}

export interface LoginResponse {
    token: string;
    tipo?: string; // "Bearer"
    correo: string;
    rol: string; // lo que devuelva el backend; se normaliza con normalizarRol()
    nombre?: string; // el backend aún no lo envía: mientras tanto se muestra el correo
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