// Sigue los DTOs de /api/usuarios (HU14). La contraseña nunca llega en las respuestas.

import { Rol } from './auth.model';

export interface Usuario {
    id: number;
    nombre: string;
    correo: string;
    rol: Rol;
    activo: boolean;
    creadoEn: string;
}

export interface UsuarioRequest {
    nombre: string;
    correo: string;
    password: string; // mínimo 8 caracteres
    rol: Rol;
}

/** PATCH /api/usuarios/{id}: los campos que no se envían no cambian. */
export interface UsuarioUpdate {
    rol?: Rol;
    activo?: boolean;
}
