import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Alerta, EstadoAlerta } from '../models/alerta.model';
import { paramsDe } from '../../shared/utils/http-params';

@Injectable({ providedIn: 'root' })
export class AlertasService {
    private readonly http = inject(HttpClient);
    private readonly url = `${API_URL}/alertas`;

    /** GET /api/alertas?estado= (HU04 · Administrador). Sin estado devuelve todas. */
    listar(estado?: EstadoAlerta): Observable<Alerta[]> {
        return this.http.get<Alerta[]>(this.url, { params: paramsDe({ estado }) });
    }

    /** PATCH /api/alertas/{id}/resolver (Administrador) */
    resolver(id: number): Observable<Alerta> {
        return this.http.patch<Alerta>(`${this.url}/${id}/resolver`, {});
    }
}
