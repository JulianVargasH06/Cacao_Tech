import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';

import { Incidencia } from '../../../core/models/incidencia.model';
import { Lote } from '../../../core/models/lote.model';
import { HistorialEstado } from '../../../core/models/producto.model';
import { ETAPAS, Transformacion } from '../../../core/models/transformacion.models';
import { IncidenciasService } from '../../../core/services/incidencias.service';
import { LotesService } from '../../../core/services/lotes.service';
import { TransformacionesService } from '../../../core/services/transformaciones.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { mensajeDeError } from '../../../shared/utils/errores-api';
import { formatoKg, formatoPorcentaje, severidadEstado } from '../../../shared/utils/formatos';

/** Un paso del recorrido: la recepción de un lote de finca o una transformación. */
interface Paso {
    fecha: string;
    tipo: 'RECEPCION' | 'TRANSFORMACION';
    lote?: Lote;
    transformacion?: Transformacion;
}

/**
 * Trazabilidad interna (HU03): el recorrido completo de un lote con todos los detalles
 * (kg, merma, responsable), lo que se produjo a partir de él, su historial de estados y sus incidencias.
 */
@Component({
    selector: 'app-trazabilidad',
    imports: [FormsModule, DatePipe, RouterLink, PageHeader, CardModule, InputTextModule, ButtonModule, MessageModule, TagModule, TimelineModule],
    template: `
    <section class="page">
      <app-page-header
        titulo="Trazabilidad"
        descripcion="Recorrido completo de un lote: de dónde viene, qué se produjo con él y qué le ha pasado"
        historia="HU03"
        rol="Administrador"
      />

      <p-card>
        <form class="buscador" (ngSubmit)="buscar()">
          <input pInputText name="codigo" [(ngModel)]="busqueda" list="codigos-lote" placeholder="Código del lote o producto"
            aria-label="Código del lote" />
          <datalist id="codigos-lote">
            @for (l of lotes(); track l.id) {
              <option [value]="l.codigo"></option>
            }
          </datalist>
          <p-button type="submit" label="Rastrear" icon="pi pi-search" [loading]="cargando()" [disabled]="!busqueda.trim()" />
        </form>
      </p-card>

      @if (error()) {
        <p-message severity="error" [text]="error()" />
      }
      @if (noEncontrado()) {
        <p-message severity="warn" [text]="'No hay ningún lote con el código ' + noEncontrado()" />
      }

      @if (lote(); as l) {
        <p-card>
          <div class="resumen">
            <div>
              <span class="tipo">{{ l.tipoLote === 'RECEPCION' ? 'Lote recibido de finca' : 'Lote resultante de una transformación' }}</span>
              <h2>{{ l.codigo }}</h2>
              <span class="muted">{{ l.productor }} · {{ l.origen }} · registrado el {{ l.fechaRegistro | date: 'dd/MM/yyyy' }}</span>
            </div>
            <div class="resumen-derecha">
              <p-tag [value]="l.estado" [severity]="severidad(l.estado)" [rounded]="true" />
              <a [href]="'/publico/producto/' + l.codigo" target="_blank" rel="noopener" class="enlace">
                Ficha pública <i class="pi pi-external-link"></i>
              </a>
            </div>
          </div>
          <div class="cifras">
            <div><span>Inicial</span><strong>{{ kg(l.cantidadInicial) }} kg</strong></div>
            <div><span>Consumida</span><strong>{{ kg(l.cantidadInicial - l.cantidadDisponible) }} kg</strong></div>
            <div><span>Disponible</span><strong>{{ kg(l.cantidadDisponible) }} kg</strong></div>
          </div>
        </p-card>

        <div class="columnas">
          <p-card header="De dónde viene">
            <p-timeline [value]="recorrido()" class="recorrido">
              <ng-template #marker let-p>
                <span class="marcador" [class.marcador-inicio]="p.tipo === 'RECEPCION'">
                  <i [class]="p.tipo === 'RECEPCION' ? 'pi pi-home' : 'pi pi-sync'"></i>
                </span>
              </ng-template>
              <ng-template #content let-p>
                @if (p.tipo === 'RECEPCION') {
                  <div class="paso">
                    <strong>Recepción · {{ p.lote.codigo }}</strong>
                    <small>{{ p.fecha | date: 'dd/MM/yyyy' }}</small>
                    <p>{{ kg(p.lote.cantidadInicial) }} kg de {{ p.lote.productor }} ({{ p.lote.origen }})</p>
                  </div>
                } @else {
                  <div class="paso">
                    <strong>{{ etapa(p.transformacion.tipo) }} · {{ p.transformacion.codigo }}</strong>
                    <small>{{ p.fecha | date: 'dd/MM/yyyy HH:mm' }} · {{ p.transformacion.responsable }}</small>
                    <p>
                      @for (e of p.transformacion.entradas; track e.loteId; let ultimo = $last) {
                        {{ codigoDe(e.loteId, e.codigoLote) }} ({{ kg(e.cantidadKg) }} kg){{ ultimo ? '' : ', ' }}
                      }
                      →
                      @for (s of p.transformacion.salidas; track s.codigo; let ultimo = $last) {
                        {{ s.codigo }} ({{ kg(s.cantidadKg) }} kg){{ ultimo ? '' : ', ' }}
                      }
                    </p>
                    <p-tag
                      [value]="'Merma ' + porcentaje(p.transformacion.mermaPorcentaje)"
                      [severity]="(p.transformacion.mermaPorcentaje ?? 0) > 15 ? 'danger' : 'success'"
                      [rounded]="true"
                    />
                  </div>
                }
              </ng-template>
            </p-timeline>
          </p-card>

          <div class="columna">
            <p-card header="Qué se produjo con él">
              @if (destinos().length === 0) {
                <p class="vacio">Este lote todavía no se ha usado en ninguna transformación</p>
              }
              <ul class="lista">
                @for (d of destinos(); track d.id) {
                  <li>
                    <a [routerLink]="[]" [queryParams]="{ codigo: d.codigo }">{{ d.codigo }}</a>
                    <p-tag [value]="d.estado" [severity]="severidad(d.estado)" [rounded]="true" />
                    <small>{{ kg(d.cantidadDisponible) }} kg disponibles</small>
                  </li>
                }
              </ul>
            </p-card>

            <p-card header="Historial de estados">
              @if (historial().length === 0) {
                <p class="vacio">Sin cambios de estado desde su registro</p>
              }
              <ul class="lista">
                @for (h of historial(); track h.id) {
                  <li class="bloque">
                    <span>{{ h.estadoAnterior ?? '—' }} → <strong>{{ h.estadoNuevo }}</strong></span>
                    <p>{{ h.motivo }}</p>
                    <small>{{ h.usuario }} · {{ h.fecha | date: 'dd/MM/yyyy HH:mm' }}</small>
                  </li>
                }
              </ul>
            </p-card>

            <p-card header="Incidencias">
              @if (incidencias().length === 0) {
                <p class="vacio">No se han reportado incidencias sobre este lote</p>
              }
              <ul class="lista">
                @for (i of incidencias(); track i.id) {
                  <li class="bloque">
                    <span><strong>{{ i.tipo }}</strong> en {{ i.codigoTransformacion }}</span>
                    <p>{{ i.descripcion }}</p>
                    <small>{{ i.usuario }} · {{ i.fecha | date: 'dd/MM/yyyy HH:mm' }}</small>
                  </li>
                }
              </ul>
            </p-card>
          </div>
        </div>
      }
    </section>
  `,
    styles: `
    .buscador {
      display: flex;
      gap: 0.5rem;
      max-width: 32rem;
    }
    .buscador input {
      flex: 1;
      min-width: 0;
    }
    .resumen {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .resumen-derecha {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 0.5rem;
    }
    .tipo {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--p-text-muted-color);
    }
    h2 {
      margin: 0.2rem 0;
      font-size: 1.5rem;
      color: var(--p-primary-900);
    }
    .muted,
    small {
      color: var(--p-text-muted-color);
    }
    .enlace {
      font-size: 0.875rem;
      color: var(--p-primary-700);
      text-decoration: none;
    }
    .cifras {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1rem;
      margin-top: 1rem;
    }
    .cifras div {
      display: flex;
      flex-direction: column;
    }
    .cifras span {
      font-size: 0.85rem;
      color: var(--p-text-muted-color);
    }
    .columnas {
      display: grid;
      grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
      gap: 1rem;
      align-items: start;
    }
    .columna {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .marcador {
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: var(--p-primary-600);
      color: #fff;
      font-size: 0.85rem;
    }
    .marcador-inicio {
      background: var(--p-green-600);
    }
    .paso {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 0.2rem;
      padding-bottom: 1rem;
    }
    .paso p {
      margin: 0.15rem 0;
    }
    .lista {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .lista li {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    .lista li.bloque {
      display: block;
    }
    .lista p {
      margin: 0.2rem 0;
    }
    .lista a {
      font-weight: 600;
      color: var(--p-primary-700);
    }
    .vacio {
      color: var(--p-text-muted-color);
      margin: 0;
    }
    :host ::ng-deep .recorrido .p-timeline-event-opposite {
      display: none;
    }
    @media (max-width: 900px) {
      .columnas {
        grid-template-columns: 1fr;
      }
    }
  `
})
export class Trazabilidad implements OnInit {
    private readonly lotesService = inject(LotesService);
    private readonly transformacionesService = inject(TransformacionesService);
    private readonly incidenciasService = inject(IncidenciasService);
    private readonly ruta = inject(ActivatedRoute);
    private readonly router = inject(Router);

    protected busqueda = '';
    protected readonly lotes = signal<Lote[]>([]);
    protected readonly transformaciones = signal<Transformacion[]>([]);
    protected readonly lote = signal<Lote | null>(null);
    protected readonly historial = signal<HistorialEstado[]>([]);
    protected readonly incidencias = signal<Incidencia[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly noEncontrado = signal('');

    protected readonly kg = formatoKg;
    protected readonly severidad = severidadEstado;
    protected readonly porcentaje = (v?: number) => formatoPorcentaje(v);

    /** Hacia atrás: transformaciones que llevaron a este lote, hasta los lotes de finca. Orden cronológico. */
    protected readonly recorrido = computed<Paso[]>(() => {
        const inicio = this.lote();
        if (!inicio) return [];
        const pasos: Paso[] = [];
        const visitados = new Set<string>();
        const pendientes = [inicio.codigo];
        while (pendientes.length) {
            const codigo = pendientes.pop()!;
            if (visitados.has(codigo)) continue;
            visitados.add(codigo);
            const origen = this.transformaciones().find((t) => t.salidas.some((s) => s.codigo === codigo));
            if (!origen) {
                const lote = this.lotes().find((l) => l.codigo === codigo);
                if (lote) pasos.push({ fecha: `${lote.fechaRegistro}T00:00:00`, tipo: 'RECEPCION', lote });
                continue;
            }
            if (!pasos.some((p) => p.transformacion?.id === origen.id)) {
                pasos.push({ fecha: origen.fecha, tipo: 'TRANSFORMACION', transformacion: origen });
            }
            origen.entradas.forEach((e) => pendientes.push(this.codigoDe(e.loteId, e.codigoLote)));
        }
        return pasos.sort((a, b) => a.fecha.localeCompare(b.fecha));
    });

    /** Hacia adelante: todos los lotes que se produjeron (directa o indirectamente) a partir de este. */
    protected readonly destinos = computed<Lote[]>(() => {
        const inicio = this.lote();
        if (!inicio) return [];
        const encontrados = new Map<number, Lote>();
        const pendientes = [inicio.id];
        while (pendientes.length) {
            const id = pendientes.pop()!;
            for (const t of this.transformaciones().filter((x) => x.entradas.some((e) => e.loteId === id))) {
                for (const s of t.salidas) {
                    const lote = this.lotes().find((l) => l.codigo === s.codigo);
                    if (lote && !encontrados.has(lote.id)) {
                        encontrados.set(lote.id, lote);
                        pendientes.push(lote.id);
                    }
                }
            }
        }
        return [...encontrados.values()];
    });

    ngOnInit(): void {
        forkJoin({ lotes: this.lotesService.listar(), transformaciones: this.transformacionesService.listar() }).subscribe({
            next: ({ lotes, transformaciones }) => {
                this.lotes.set(lotes);
                this.transformaciones.set(transformaciones);
                this.cargando.set(false);
                // ?codigo= en la URL: se puede compartir el enlace de una trazabilidad
                this.ruta.queryParamMap.subscribe((params) => {
                    const codigo = params.get('codigo');
                    if (codigo) {
                        this.busqueda = codigo;
                        this.mostrar(codigo);
                    }
                });
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudo cargar la información: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    protected buscar(): void {
        const codigo = this.busqueda.trim();
        if (codigo) this.router.navigate([], { queryParams: { codigo: codigo.toUpperCase() } });
    }

    protected etapa(tipo: string): string {
        return ETAPAS.find((e) => e.valor === tipo)?.etiqueta ?? tipo;
    }

    /** El backend manda el código del lote de entrada; si no viene, se busca por id. */
    protected codigoDe(loteId: number, codigoLote?: string): string {
        return codigoLote ?? this.lotes().find((l) => l.id === loteId)?.codigo ?? `#${loteId}`;
    }

    private mostrar(codigo: string): void {
        const lote = this.lotes().find((l) => l.codigo.toLowerCase() === codigo.toLowerCase());
        this.noEncontrado.set(lote ? '' : codigo);
        this.lote.set(lote ?? null);
        this.historial.set([]);
        this.incidencias.set([]);
        if (!lote) return;
        forkJoin({
            historial: this.lotesService.historialEstados(lote.id),
            incidencias: this.incidenciasService.listar({ loteId: lote.id })
        }).subscribe({
            next: ({ historial, incidencias }) => {
                this.historial.set(historial);
                this.incidencias.set(incidencias);
            },
            error: (err: HttpErrorResponse) => this.error.set('No se pudo cargar el historial: ' + mensajeDeError(err))
        });
    }
}
