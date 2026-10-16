import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Alerta } from '../models/alerta.model';

@Injectable({ providedIn: 'root' })
export class AlertasService {
    private readonly http = inject(HttpClient);

    /** GET /api/alertas (HU04 y HU05 · Administrador) */
    listar(): Observable<Alerta[]> {
        return this.http.get<Alerta[]>(`${API_URL}/alertas`);
    }
}