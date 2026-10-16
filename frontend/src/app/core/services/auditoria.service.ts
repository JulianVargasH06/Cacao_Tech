import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';

import { API_URL } from '../config/api.config';
import { USAR_MOCK_AUDITORIA } from '../config/dev.config';
import { FiltroAuditoria, RegistroAuditoria } from '../models/auditoria.model';
import { paramsDe } from '../../shared/utils/http-params';
import { LotesService } from './lotes.service';
import { TransformacionesService } from './transformaciones.service';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
    private readonly http = inject(HttpClient);
    private readonly lotes = inject(LotesService);
    private readonly transformaciones = inject(TransformacionesService);
    private readonly url = `${API_URL}/auditoria`;

    /** GET /api/auditoria (HU10 · solo Administrador) */
    listar(filtro: FiltroAuditoria = {}): Observable<RegistroAuditoria[]> {
        if (USAR_MOCK_AUDITORIA) return this.listarSimulado();
        return this.http.get<RegistroAuditoria[]>(this.url, { params: paramsDe(filtro) });
    }

    /**
     * Mientras no exista GET /api/auditoria, arma la auditoría a partir de los lotes y transformaciones
     * (reales o simulados, según USAR_MOCK_API), más el cambio de estado de cada lote bloqueado.
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
                            usuario: origen?.responsable ?? 'operador',
                            entidad: 'Lote',
                            entidadId: l.id,
                            accion: 'CREAR',
                            valoresNuevos: JSON.stringify({ codigo: l.codigo, cantidadInicial: l.cantidadInicial, tipoLote: l.tipoLote }),
                            fecha: origen?.fecha ?? `${l.fechaRegistro}T07:00:00`
                        };
                    }),
                    ...transformaciones.map((t) => ({
                        id: 0,
                        usuario: t.responsable,
                        entidad: 'Transformacion',
                        entidadId: t.id,
                        accion: 'CREAR',
                        valoresNuevos: JSON.stringify({ codigo: t.codigo, tipo: t.tipo }),
                        fecha: t.fecha
                    })),
                    // Cada lote bloqueado tuvo que pasar por un cambio de estado
                    ...lotes
                        .filter((l) => l.estado === 'BLOQUEADO')
                        .map((l) => ({
                            id: 0,
                            usuario: 'admin',
                            entidad: 'Lote',
                            entidadId: l.id,
                            accion: 'CAMBIAR_ESTADO',
                            valoresAnteriores: JSON.stringify({ estado: 'PENDIENTE' }),
                            valoresNuevos: JSON.stringify({ estado: 'BLOQUEADO' }),
                            fecha: `${l.fechaRegistro}T09:30:00`
                        }))
                ];
                return registros
                    .sort((a, b) => b.fecha.localeCompare(a.fecha))
                    .map((r, i) => ({ ...r, id: i + 1 }));
            })
        );
    }
}
