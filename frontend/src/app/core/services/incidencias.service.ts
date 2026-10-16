import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Incidencia, IncidenciaRequest } from '../models/incidencia.model';
import { paramsDe } from '../../shared/utils/http-params';

@Injectable({ providedIn: 'root' })
export class IncidenciasService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/incidencias`;

    /** GET /api/incidencias?transformacionId=&loteId= (ambos opcionales) */
    listar(filtro: { transformacionId?: number; loteId?: number } = {}): Observable<Incidencia[]> {
        return this.http.get<Incidencia[]>(this.url, { params: paramsDe(filtro) });
    }

    /** POST /api/incidencias (RF03): con bloquearLote = true bloquea el lote y genera una alerta. */
    registrar(datos: IncidenciaRequest): Observable<Incidencia> {
        return this.http.post<Incidencia>(this.url, datos);
    }
}
