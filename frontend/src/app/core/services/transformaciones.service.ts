import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, delay, map, of, timer } from 'rxjs';

import { API_URL } from '../config/api.config';
import { USAR_MOCK_API } from '../config/dev.config';
import { Transformacion, TransformacionRequest } from '../models/transformacion.models';
import { AuthService } from './auth.service';
import { LotesService } from './lotes.service';

@Injectable({ providedIn: 'root' })
export class TransformacionesService {
    private readonly http = inject(HttpClient);
    private readonly lotes = inject(LotesService);
    private readonly auth = inject(AuthService);
    private readonly url = `${API_URL}/transformaciones`;

    /** Datos de prueba mientras USAR_MOCK_API = true. Se pierden al recargar la página. */
    private readonly simuladas: Transformacion[] = [
        {
            // Explica los 80 kg consumidos de L-001 en los datos de prueba de LotesService
            id: 1,
            codigo: 'TR-001',
            tipo: 'FERMENTACION',
            fecha: '2026-10-11T08:30:00',
            entradas: [{ loteId: 1, cantidadKg: 80 }],
            salidas: [{ codigo: 'L-001-FER', cantidadKg: 72 }],
            observaciones: 'Fermentación de 6 días en cajones',
            responsable: 'operador1'
        }
    ];

    /** GET /api/transformaciones. OJO: no está en la tabla de endpoints todavía (lo usará el historial). */
    listar(): Observable<Transformacion[]> {
        if (USAR_MOCK_API) return of([...this.simuladas]).pipe(delay(300));
        return this.http.get<Transformacion[]>(this.url);
    }

    /** POST /api/transformaciones (HU02 · Operador) */
    registrar(datos: TransformacionRequest): Observable<Transformacion> {
        if (USAR_MOCK_API) return this.registrarSimulado(datos);
        return this.http.post<Transformacion>(this.url, datos);
    }

    /**
     * Imita al backend (HU02 + HU08): valida todo antes de tocar nada y, si está bien,
     * descuenta las entradas de sus lotes y crea un lote nuevo por cada salida.
     */
    private registrarSimulado(datos: TransformacionRequest): Observable<Transformacion> {
        return timer(400).pipe(
            map(() => {
                for (const entrada of datos.entradas) {
                    const lote = this.lotes.simuladoPorId(entrada.loteId);
                    if (!lote) {
                        throw this.error(404, 'El lote de entrada no existe');
                    }
                    if (lote.estado === 'BLOQUEADO') {
                        throw this.error(422, `El lote ${lote.codigo} está bloqueado y no se puede transformar`);
                    }
                    if (entrada.cantidadKg > lote.cantidadDisponible) {
                        throw this.error(422, `El lote ${lote.codigo} solo tiene ${lote.cantidadDisponible} kg disponibles`);
                    }
                }

                const totalEntradas = datos.entradas.reduce((suma, e) => suma + e.cantidadKg, 0);
                const totalSalidas = datos.salidas.reduce((suma, s) => suma + s.cantidadKg, 0);
                if (totalSalidas > totalEntradas) {
                    throw this.error(400, 'La salida no puede ser mayor que la entrada');
                }

                for (const salida of datos.salidas) {
                    if (this.lotes.existeCodigoSimulado(salida.codigo)) {
                        throw this.error(409, `Ya existe un lote con el código ${salida.codigo}`);
                    }
                }

                // Todo válido: ahora sí se modifican los lotes.
                const origen = this.lotes.simuladoPorId(datos.entradas[0].loteId)!;
                datos.entradas.forEach((e) => this.lotes.consumirSimulado(e.loteId, e.cantidadKg));
                datos.salidas.forEach((s) =>
                    this.lotes.crearSimulado(
                        {
                            codigo: s.codigo,
                            productor: origen.productor,
                            origen: origen.origen,
                            fechaRegistro: datos.fecha.slice(0, 10),
                            cantidadInicial: s.cantidadKg
                        },
                        'RESULTADO_TRANSFORMACION'
                    )
                );

                const numero = this.simuladas.length + 1;
                const transformacion: Transformacion = {
                    ...datos,
                    id: numero,
                    codigo: `TR-${String(numero).padStart(3, '0')}`,
                    responsable: this.auth.nombre()
                };
                this.simuladas.unshift(transformacion);
                return transformacion;
            })
        );
    }

    private error(status: number, mensaje: string): HttpErrorResponse {
        return new HttpErrorResponse({ status, error: { mensaje } });
    }
}
