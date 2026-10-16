import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Usuario, UsuarioRequest, UsuarioUpdate } from '../models/usuario.model';

/** Gestión de cuentas internas (HU14 · solo Administrador). */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/usuarios`;

    listar(): Observable<Usuario[]> {
        return this.http.get<Usuario[]>(this.url);
    }

    crear(datos: UsuarioRequest): Observable<Usuario> {
        return this.http.post<Usuario>(this.url, datos);
    }

    /** Cambia el rol y/o activa o desactiva la cuenta. */
    actualizar(id: number, cambios: UsuarioUpdate): Observable<Usuario> {
        return this.http.patch<Usuario>(`${this.url}/${id}`, cambios);
    }
}
