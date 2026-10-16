import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, delay, map, of, timer } from 'rxjs';

import { API_URL } from '../config/api.config';
import { USAR_MOCK_API } from '../config/dev.config';
import { Lote, LoteRequest, TrazabilidadLote } from '../models/lote.model';

@Injectable({ providedIn: 'root' })
export class LotesService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/lotes`;

    private readonly simulados: Lote[] = [
        {
            id: 1, codigo: 'L-001', productor: 'Finca La Esperanza', origen: 'Rivera, Huila',
            fechaRecepcion: '2026-10-10', cantidadRecibida: 500, cantidadDisponible: 420, unidad: 'kg'
        },
        {
            id: 2, codigo: 'L-002', productor: 'Finca El Porvenir', origen: 'Gigante, Huila',
            fechaRecepcion: '2026-10-12', cantidadRecibida: 300, cantidadDisponible: 300, unidad: 'kg'
        }
    ];

    /** GET /api/lotes */
    listar(): Observable<Lote[]> {
        if (USAR_MOCK_API) return of([...this.simulados]).pipe(delay(300));
        return this.http.get<Lote[]>(this.url);
    }

    /** GET /api/lotes/{id} */
    obtener(id: number | string): Observable<Lote> {
        return this.http.get<Lote>(`${this.url}/${id}`);
    }

    /** POST /api/lotes (HU01 · Operador) */
    registrar(datos: LoteRequest): Observable<Lote> {
        if (USAR_MOCK_API) return this.registrarSimulado(datos);
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

    /** Imita al backend: rechaza códigos repetidos (409) y deja todo lo recibido como disponible. */
    private registrarSimulado(datos: LoteRequest): Observable<Lote> {
        return timer(400).pipe(
            map(() => {
                if (this.existeCodigoSimulado(datos.codigo)) {
                    throw new HttpErrorResponse({
                        status: 409,
                        error: { mensaje: `Ya existe un lote con el código ${datos.codigo}` }
                    });
                }
                return this.crearSimulado(datos);
            })
        );
    }

    // --- Ayudas para otras simulaciones (TransformacionesService). No se usan con el backend real. ---

    simuladoPorId(id: number): Lote | undefined {
        return this.simulados.find((l) => l.id === id);
    }

    existeCodigoSimulado(codigo: string): boolean {
        return this.simulados.some((l) => l.codigo.toLowerCase() === codigo.toLowerCase());
    }

    crearSimulado(datos: LoteRequest): Lote {
        const lote: Lote = { ...datos, id: this.simulados.length + 1, cantidadDisponible: datos.cantidadRecibida };
        this.simulados.unshift(lote);
        return lote;
    }

    /** Descuenta lo que una transformación tomó del lote. */
    consumirSimulado(id: number, cantidad: number): void {
        const i = this.simulados.findIndex((l) => l.id === id);
        if (i === -1) return;
        const lote = this.simulados[i];
        const disponible = Math.round((lote.cantidadDisponible - cantidad) * 100) / 100;
        this.simulados[i] = { ...lote, cantidadDisponible: disponible };
    }
}