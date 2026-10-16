import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { FichaPublica } from '../models/producto.model';

@Injectable({ providedIn: 'root' })
export class PublicoService {
    private readonly http = inject(HttpClient);

    /** GET /api/publico/productos/{codigo} (HU06 · sin iniciar sesión) */
    obtenerFicha(codigo: string): Observable<FichaPublica> {
        return this.http.get<FichaPublica>(`${API_URL}/publico/productos/${encodeURIComponent(codigo)}`);
    }
}