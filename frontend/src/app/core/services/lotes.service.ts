import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Lote, LoteRequest, TrazabilidadLote } from '../models/lote.model';

@Injectable({ providedIn: 'root' })
export class LotesService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/lotes`;

    /** GET /api/lotes */
    listar(): Observable<Lote[]> {
        return this.http.get<Lote[]>(this.url);
    }

    /** GET /api/lotes/{id} */
    obtener(id: number | string): Observable<Lote> {
        return this.http.get<Lote>(`${this.url}/${id}`);
    }

    /** POST /api/lotes (HU01 · Operador) */
    registrar(datos: LoteRequest): Observable<Lote> {
        return this.http.post<Lote>(this.url, datos);
    }

    /** GET /api/lotes/{id}/trazabilidad (HU03 · Administrador) */
    trazabilidad(id: number | string): Observable<TrazabilidadLote> {
        return this.http.get<TrazabilidadLote>(`${this.url}/${id}/trazabilidad`);
    }

    /** GET /api/lotes/exportar (HU11 · Administrador). Devuelve el CSV como archivo. */
    exportarCsv(): Observable<Blob> {
        return this.http.get(`${this.url}/exportar`, { responseType: 'blob' });
    }
}