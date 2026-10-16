import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { CambioEstadoRequest } from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class ProductosService {
    private readonly http = inject(HttpClient);

    /** PATCH /api/productos/{id}/estado (HU09 · Administrador) */
    cambiarEstado(id: number | string, cambio: CambioEstadoRequest): Observable<unknown> {
        return this.http.patch(`${API_URL}/productos/${id}/estado`, cambio);
    }
}