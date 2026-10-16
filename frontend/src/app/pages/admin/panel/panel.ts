import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { Alerta } from '../../../core/models/alerta.model';
import { Lote } from '../../../core/models/lote.model';
import { Transformacion } from '../../../core/models/transformacion.models';
import { AlertasService } from '../../../core/services/alertas.service';
import { LotesService } from '../../../core/services/lotes.service';
import { TransformacionesService } from '../../../core/services/transformaciones.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { descargarArchivo, nombreConFecha } from '../../../shared/utils/descargas';
import { mensajeDeError } from '../../../shared/utils/errores-api';
import { etiquetaAlerta, formatoCop, formatoKg, formatoPorcentaje, severidadEstado } from '../../../shared/utils/formatos';

interface Indicador {
    etiqueta: string;
    valor: string;
    detalle: string;
    icono: string;
    alerta?: boolean;
}

/** Panel del administrador (HU04, HU05, HU11): inventario, alertas de merma, indicadores económicos y exportación. */
@Component({
    selector: 'app-panel',
    imports: [RouterLink, DatePipe, PageHeader, CardModule, TableModule, TagModule, ButtonModule, MessageModule],
    template: `
    <section class="page">
      <app-page-header
        titulo="Panel"
        descripcion="Resumen del inventario, las alertas de merma y los costos de proceso"
        historia="HU04 · HU05 · HU11"
        rol="Administrador"
      />

      @if (error()) {
        <p-message severity="error">
          <div class="mensaje-accion">
            <span>{{ error() }}</span>
            <p-button label="Reintentar" icon="pi pi-refresh" size="small" [text]="true" (onClick)="cargar()" />
          </div>
        </p-message>
      }

      <div class="indicadores">
        @for (i of indicadores(); track i.etiqueta) {
          <p-card>
            <div class="indicador" [class.indicador-alerta]="i.alerta">
              <span class="icono"><i [class]="i.icono"></i></span>
              <div>
                <span class="etiqueta">{{ i.etiqueta }}</span>
                <strong>{{ cargando() ? '…' : i.valor }}</strong>
                <small>{{ i.detalle }}</small>
              </div>
            </div>
          </p-card>
        }
      </div>

      <div class="columnas">
        <p-card>
          <ng-template #title>
            <div class="titulo-tarjeta">
              <span>Alertas activas</span>
              <a routerLink="/admin/mermas-costos" class="enlace">Ver mermas y costos <i class="pi pi-arrow-right"></i></a>
            </div>
          </ng-template>
          @if (!cargando() && alertas().length === 0) {
            <p class="vacio"><i class="pi pi-check-circle"></i> No hay alertas activas</p>
          }
          <ul class="alertas">
            @for (a of alertas().slice(0, 5); track a.id) {
              <li>
                <p-tag [value]="etiquetaAlerta(a.tipo)" severity="danger" [rounded]="true" />
                <div>
                  <p>{{ a.descripcion }}</p>
                  <small>{{ a.codigoLote }} · {{ a.creadaEn | date: 'dd/MM/yyyy HH:mm' }}</small>
                </div>
              </li>
            }
          </ul>
        </p-card>

        <p-card>
          <ng-template #title>
            <div class="titulo-tarjeta">
              <span>Lotes recientes</span>
              <div class="acciones">
                <a routerLink="/admin/auditoria" class="enlace">Filtrar</a>
                <p-button
                  label="Exportar CSV"
                  icon="pi pi-download"
                  severity="secondary"
                  [outlined]="true"
                  size="small"
                  [loading]="exportando()"
                  (onClick)="exportar()"
                />
              </div>
            </div>
          </ng-template>
          <p-table [value]="lotes().slice(0, 6)" [loading]="cargando()" [tableStyle]="{ 'min-width': '28rem' }">
            <ng-template #header>
              <tr>
                <th>Lote</th>
                <th>Productor</th>
                <th class="num">Disponible</th>
                <th>Estado</th>
              </tr>
            </ng-template>
            <ng-template #body let-lote>
              <tr>
                <td class="sin-salto"><strong>{{ lote.codigo }}</strong></td>
                <td>{{ lote.productor }}</td>
                <td class="num">{{ kg(lote.cantidadDisponible) }} kg</td>
                <td><p-tag [value]="lote.estado" [severity]="severidad(lote.estado)" [rounded]="true" /></td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr><td colspan="4" class="vacio">Aún no hay lotes registrados</td></tr>
            </ng-template>
          </p-table>
        </p-card>
      </div>
    </section>
  `,
    styles: `
    .indicadores {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      gap: 1rem;
    }
    .indicador {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .indicador div {
      display: flex;
      flex-direction: column;
    }
    .icono {
      width: 3rem;
      height: 3rem;
      flex-shrink: 0;
      border-radius: 1rem;
      display: grid;
      place-items: center;
      background: var(--p-primary-100);
      color: var(--p-primary-700);
      font-size: 1.25rem;
    }
    .indicador-alerta .icono {
      background: var(--p-red-100);
      color: var(--p-red-600);
    }
    .etiqueta,
    small {
      color: var(--p-text-muted-color);
    }
    .etiqueta {
      font-size: 0.85rem;
    }
    strong {
      font-size: 1.5rem;
      color: var(--p-primary-900);
    }
    td strong {
      font-size: inherit;
    }
    .columnas {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(22rem, 1fr));
      gap: 1rem;
      align-items: start;
    }
    .titulo-tarjeta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .acciones {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .enlace {
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--p-primary-700);
      text-decoration: none;
    }
    .alertas {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.9rem;
    }
    .alertas li {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
    }
    .alertas p {
      margin: 0 0 0.2rem;
      font-size: 0.9rem;
    }
    .vacio {
      text-align: center;
      padding: 1.5rem;
      color: var(--p-text-muted-color);
    }
    .num {
      text-align: right;
      white-space: nowrap;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .mensaje-accion {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr {
      background: transparent;
    }
  `
})
export class Panel implements OnInit {
    private readonly lotesService = inject(LotesService);
    private readonly transformacionesService = inject(TransformacionesService);
    private readonly alertasService = inject(AlertasService);

    protected readonly lotes = signal<Lote[]>([]);
    protected readonly transformaciones = signal<Transformacion[]>([]);
    protected readonly alertas = signal<Alerta[]>([]);
    protected readonly cargando = signal(true);
    protected readonly exportando = signal(false);
    protected readonly error = signal('');

    protected readonly kg = formatoKg;
    protected readonly etiquetaAlerta = etiquetaAlerta;
    protected readonly severidad = severidadEstado;

    /** Los indicadores se recalculan solos cuando llegan los datos. */
    protected readonly indicadores = computed<Indicador[]>(() => {
        const lotes = this.lotes();
        const conExistencias = lotes.filter((l) => l.cantidadDisponible > 0);
        const disponible = conExistencias.reduce((suma, l) => suma + l.cantidadDisponible, 0);
        const bloqueados = lotes.filter((l) => l.estado === 'BLOQUEADO').length;

        // Merma ponderada: total perdido / total que entró (una transformación grande pesa más que una pequeña)
        const transformaciones = this.transformaciones();
        const entro = transformaciones.reduce((s, t) => s + t.entradas.reduce((x, e) => x + e.cantidadKg, 0), 0);
        const perdido = transformaciones.reduce((s, t) => s + (t.mermaKg ?? 0), 0);
        const mermaPromedio = entro > 0 ? (perdido / entro) * 100 : null;

        const costoTotal = transformaciones.reduce((s, t) => s + (t.costoTotalCop ?? 0), 0);
        const kgConCosto = transformaciones
            .filter((t) => (t.costoTotalCop ?? 0) > 0)
            .reduce((s, t) => s + t.salidas.reduce((x, salida) => x + salida.cantidadKg, 0), 0);

        return [
            {
                etiqueta: 'Inventario disponible',
                valor: `${formatoKg(disponible)} kg`,
                detalle: `${conExistencias.length} lotes con existencias`,
                icono: 'pi pi-database'
            },
            {
                etiqueta: 'Alertas activas',
                valor: String(this.alertas().length),
                detalle: `${bloqueados} lotes bloqueados`,
                icono: 'pi pi-bell',
                alerta: this.alertas().length > 0
            },
            {
                etiqueta: 'Merma promedio',
                valor: formatoPorcentaje(mermaPromedio),
                detalle: `en ${transformaciones.length} transformaciones · umbral 15 %`,
                icono: 'pi pi-percentage',
                alerta: (mermaPromedio ?? 0) > 15
            },
            {
                etiqueta: 'Costo de proceso',
                valor: formatoCop(costoTotal),
                detalle: kgConCosto > 0 ? `${formatoCop(costoTotal / kgConCosto)} por kg producido` : 'aún sin costos registrados',
                icono: 'pi pi-wallet'
            }
        ];
    });

    ngOnInit(): void {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set('');
        forkJoin({
            lotes: this.lotesService.listar(),
            transformaciones: this.transformacionesService.listar(),
            alertas: this.alertasService.listar('ACTIVA')
        }).subscribe({
            next: ({ lotes, transformaciones, alertas }) => {
                this.lotes.set(lotes);
                this.transformaciones.set(transformaciones);
                this.alertas.set(alertas);
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudo cargar el panel: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    /** HU11: descarga todos los lotes en CSV (los filtros están en Auditoría y exportación). */
    protected exportar(): void {
        this.exportando.set(true);
        this.lotesService.exportarCsv().subscribe({
            next: (archivo) => {
                descargarArchivo(archivo, nombreConFecha('lotes', 'csv'));
                this.exportando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudo exportar: ' + mensajeDeError(err));
                this.exportando.set(false);
            }
        });
    }
}
