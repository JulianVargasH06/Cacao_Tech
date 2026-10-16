import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';

import { API_URL } from '../config/api.config';
import { USAR_MOCK_API } from '../config/dev.config';
import { RegistroAuditoria } from '../models/auditoria.model';
import { LotesService } from './lotes.service';
import { TransformacionesService } from './transformaciones.service';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
    private readonly http = inject(HttpClient);
    private readonly lotes = inject(LotesService);
    private readonly transformaciones = inject(TransformacionesService);
    private readonly url = `${API_URL}/auditoria`;

    /** GET /api/auditoria (HU10 · Administrador). OJO: no está en la tabla de endpoints todavía. */
    listar(): Observable<RegistroAuditoria[]> {
        if (USAR_MOCK_API) return this.listarSimulado();
        return this.http.get<RegistroAuditoria[]>(this.url);
    }

    /**
     * Imita al backend armando la auditoría a partir de los lotes y transformaciones simulados,
     * más un cambio de estado de ejemplo (el bloqueo de L-003).
     */
    private listarSimulado(): Observable<RegistroAuditoria[]> {
        return forkJoin([this.lotes.listar(), this.transformaciones.listar()]).pipe(
            map(([lotes, transformaciones]) => {
                const registros: RegistroAuditoria[] = [
                    ...lotes.map((l) => {
                        // Un lote resultante se crea en el mismo momento que la transformación que lo produjo.
                        const origen = transformaciones.find((t) => t.salidas.some((s) => s.codigo === l.codigo));
                        return {
                            id: 0,
                            usuario: origen?.responsable ?? 'operador1',
                            entidad: 'Lote',
                            entidadId: l.id,
                            accion: 'CREAR',
                            valoresNuevos: `codigo=${l.codigo}, cantidadInicial=${l.cantidadInicial} kg`,
                            fecha: origen?.fecha ?? `${l.fechaRegistro}T07:00:00`
                        };
                    }),
                    ...transformaciones.map((t) => ({
                        id: 0,
                        usuario: t.responsable,
                        entidad: 'Transformacion',
                        entidadId: t.id,
                        accion: 'CREAR',
                        valoresNuevos: `codigo=${t.codigo}, tipo=${t.tipo}`,
                        fecha: t.fecha
                    })),
                    {
                        id: 0,
                        usuario: 'admin',
                        entidad: 'Lote',
                        entidadId: 3,
                        accion: 'CAMBIAR_ESTADO',
                        valoresAnteriores: 'estado=PENDIENTE',
                        valoresNuevos: 'estado=BLOQUEADO (humedad fuera de rango)',
                        fecha: '2026-10-14T09:30:00'
                    }
                ];
                return registros
                    .sort((a, b) => b.fecha.localeCompare(a.fecha))
                    .map((r, i) => ({ ...r, id: i + 1 }));
            })
        );
    }
}
