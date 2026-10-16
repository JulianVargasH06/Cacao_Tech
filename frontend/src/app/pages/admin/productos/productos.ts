import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';

import { EstadoLote, Lote, TipoLote } from '../../../core/models/lote.model';
import { HistorialEstado } from '../../../core/models/producto.model';
import { LotesService } from '../../../core/services/lotes.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { mensajeDeError } from '../../../shared/utils/errores-api';
import { formatoKg, severidadEstado } from '../../../shared/utils/formatos';

/** Cambio de estado que el administrador está a punto de confirmar. */
interface CambioPendiente {
    lote: Lote;
    estado: 'LIBERADO' | 'BLOQUEADO';
}

/** Gestión de productos (HU09): liberar o bloquear lotes con un motivo obligatorio y ver su historial de estados. */
@Component({
    selector: 'app-productos',
    imports: [
        FormsModule,
        DatePipe,
        PageHeader,
        CardModule,
        TableModule,
        TagModule,
        ButtonModule,
        InputTextModule,
        SelectModule,
        DialogModule,
        TextareaModule,
        MessageModule,
        ToastModule
    ],
    providers: [MessageService],
    template: `
    <section class="page">
      <p-toast />
      <app-page-header
        titulo="Gestión de productos"
        descripcion="Libera o bloquea lotes para la venta. Cada cambio exige un motivo y queda en el historial"
        historia="HU09"
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

      <p-card>
        <p-table
          [value]="lotesFiltrados()"
          [loading]="cargando()"
          [paginator]="true"
          [rows]="10"
          [tableStyle]="{ 'min-width': '58rem' }"
        >
          <ng-template #caption>
            <div class="barra">
              <input
                pInputText
                type="search"
                placeholder="Buscar por código o productor"
                [ngModel]="busqueda()"
                (ngModelChange)="busqueda.set($event)"
              />
              <p-select
                [options]="estados"
                [ngModel]="filtroEstado()"
                (ngModelChange)="filtroEstado.set($event)"
                optionLabel="etiqueta"
                optionValue="valor"
                placeholder="Todos los estados"
                [showClear]="true"
              />
              <p-select
                [options]="tipos"
                [ngModel]="filtroTipo()"
                (ngModelChange)="filtroTipo.set($event)"
                optionLabel="etiqueta"
                optionValue="valor"
                placeholder="Todos los tipos"
                [showClear]="true"
              />
              <span class="total">{{ lotesFiltrados().length }} de {{ lotes().length }} lotes</span>
            </div>
          </ng-template>

          <ng-template #header>
            <tr>
              <th>Código</th>
              <th>Productor</th>
              <th>Tipo</th>
              <th>Registro</th>
              <th class="num">Disponible</th>
              <th>Estado</th>
              <th class="acciones-col">Acciones</th>
            </tr>
          </ng-template>

          <ng-template #body let-lote>
            <tr>
              <td class="sin-salto"><strong>{{ lote.codigo }}</strong></td>
              <td>{{ lote.productor || '—' }}</td>
              <td>{{ lote.tipoLote === 'RECEPCION' ? 'Recepción' : 'Transformado' }}</td>
              <td class="sin-salto">{{ lote.fechaRegistro | date: 'dd/MM/yyyy' }}</td>
              <td class="num">{{ kg(lote.cantidadDisponible) }} kg</td>
              <td><p-tag [value]="lote.estado" [severity]="severidad(lote.estado)" [rounded]="true" /></td>
              <td class="acciones-col">
                @if (lote.estado !== 'LIBERADO') {
                  <p-button label="Liberar" icon="pi pi-check" size="small" severity="success" [outlined]="true"
                    (onClick)="pedirCambio(lote, 'LIBERADO')" />
                }
                @if (lote.estado !== 'BLOQUEADO') {
                  <p-button label="Bloquear" icon="pi pi-ban" size="small" severity="danger" [outlined]="true"
                    (onClick)="pedirCambio(lote, 'BLOQUEADO')" />
                }
                <p-button icon="pi pi-history" size="small" severity="secondary" [text]="true" [rounded]="true"
                  ariaLabel="Historial de estados" (onClick)="verHistorial(lote)" />
                <a [href]="'/publico/producto/' + lote.codigo" target="_blank" rel="noopener" class="ficha"
                  title="Ver ficha pública"><i class="pi pi-external-link"></i></a>
              </td>
            </tr>
          </ng-template>

          <ng-template #emptymessage>
            <tr><td colspan="7" class="vacio">Ningún lote coincide con el filtro</td></tr>
          </ng-template>
        </p-table>
      </p-card>

      <!-- Confirmar cambio de estado: el motivo es obligatorio (HU09) -->
      <p-dialog
        [header]="cambio()?.estado === 'LIBERADO' ? 'Liberar lote' : 'Bloquear lote'"
        [visible]="!!cambio()"
        (visibleChange)="!$event && cerrarCambio()"
        [modal]="true"
        [style]="{ width: 'min(30rem, 95vw)' }"
      >
        @if (cambio(); as c) {
          <p class="explicacion">
            @if (c.estado === 'LIBERADO') {
              El lote <strong>{{ c.lote.codigo }}</strong> quedará disponible para la venta y para nuevas transformaciones.
            } @else {
              El lote <strong>{{ c.lote.codigo }}</strong> no se podrá transformar ni vender hasta que se libere.
            }
          </p>
          <label for="motivo">Motivo <span class="obligatorio">*</span></label>
          <textarea
            pTextarea
            id="motivo"
            [(ngModel)]="motivo"
            rows="3"
            [autoResize]="true"
            placeholder="Ej: revisado en laboratorio, humedad dentro del rango"
          ></textarea>
          @if (intentoSinMotivo() && !motivo.trim()) {
            <small class="error">Escribe el motivo del cambio</small>
          }
        }
        <ng-template #footer>
          <p-button label="Cancelar" severity="secondary" [text]="true" (onClick)="cerrarCambio()" />
          <p-button
            [label]="cambio()?.estado === 'LIBERADO' ? 'Liberar' : 'Bloquear'"
            [severity]="cambio()?.estado === 'LIBERADO' ? 'success' : 'danger'"
            [loading]="guardando()"
            (onClick)="confirmarCambio()"
          />
        </ng-template>
      </p-dialog>

      <!-- Historial de estados de un lote -->
      <p-dialog
        [header]="'Historial de ' + (loteHistorial()?.codigo ?? '')"
        [visible]="!!loteHistorial()"
        (visibleChange)="!$event && loteHistorial.set(null)"
        [modal]="true"
        [style]="{ width: 'min(36rem, 95vw)' }"
      >
        @if (cargandoHistorial()) {
          <p class="vacio">Cargando…</p>
        } @else if (historial().length === 0) {
          <p class="vacio">Este lote no ha cambiado de estado desde que se registró</p>
        } @else {
          <ul class="historial">
            @for (h of historial(); track h.id) {
              <li>
                <div class="transicion">
                  <p-tag [value]="h.estadoAnterior ?? '—'" [severity]="severidad(h.estadoAnterior ?? '')" />
                  <i class="pi pi-arrow-right"></i>
                  <p-tag [value]="h.estadoNuevo" [severity]="severidad(h.estadoNuevo)" />
                </div>
                <p>{{ h.motivo }}</p>
                <small>{{ h.usuario }} · {{ h.fecha | date: 'dd/MM/yyyy HH:mm' }}</small>
              </li>
            }
          </ul>
        }
      </p-dialog>
    </section>
  `,
    styles: `
    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem;
    }
    .barra input {
      width: min(100%, 18rem);
    }
    .total {
      margin-left: auto;
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
    }
    .num {
      text-align: right;
      white-space: nowrap;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .acciones-col {
      white-space: nowrap;
    }
    .acciones-col p-button {
      margin-right: 0.35rem;
    }
    .ficha {
      color: var(--p-primary-600);
      margin-left: 0.25rem;
    }
    .vacio {
      text-align: center;
      padding: 1.5rem;
      color: var(--p-text-muted-color);
    }
    .explicacion {
      margin-top: 0;
    }
    label {
      display: block;
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--p-primary-900);
      margin-bottom: 0.4rem;
    }
    textarea {
      width: 100%;
    }
    .obligatorio,
    .error {
      color: var(--p-red-500);
    }
    .historial {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .historial p {
      margin: 0.4rem 0 0.15rem;
    }
    .historial small {
      color: var(--p-text-muted-color);
    }
    .transicion {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .mensaje-accion {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    :host ::ng-deep .p-datatable-header,
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr,
    :host ::ng-deep .p-paginator {
      background: transparent;
    }
  `
})
export class Productos implements OnInit {
    private readonly lotesService = inject(LotesService);
    private readonly mensajes = inject(MessageService);

    protected readonly estados: { valor: EstadoLote; etiqueta: string }[] = [
        { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
        { valor: 'LIBERADO', etiqueta: 'Liberado' },
        { valor: 'BLOQUEADO', etiqueta: 'Bloqueado' }
    ];
    protected readonly tipos: { valor: TipoLote; etiqueta: string }[] = [
        { valor: 'RECEPCION', etiqueta: 'Recepción' },
        { valor: 'RESULTADO_TRANSFORMACION', etiqueta: 'Transformado' }
    ];

    protected readonly lotes = signal<Lote[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly busqueda = signal('');
    protected readonly filtroEstado = signal<EstadoLote | null>(null);
    protected readonly filtroTipo = signal<TipoLote | null>(null);

    protected readonly cambio = signal<CambioPendiente | null>(null);
    protected motivo = '';
    protected readonly intentoSinMotivo = signal(false);
    protected readonly guardando = signal(false);

    protected readonly loteHistorial = signal<Lote | null>(null);
    protected readonly historial = signal<HistorialEstado[]>([]);
    protected readonly cargandoHistorial = signal(false);

    protected readonly kg = formatoKg;
    protected readonly severidad = severidadEstado;

    protected readonly lotesFiltrados = computed(() => {
        const texto = this.busqueda().trim().toLowerCase();
        const estado = this.filtroEstado();
        const tipo = this.filtroTipo();
        return this.lotes().filter(
            (l) =>
                (!texto || `${l.codigo} ${l.productor ?? ''}`.toLowerCase().includes(texto)) &&
                (!estado || l.estado === estado) &&
                (!tipo || l.tipoLote === tipo)
        );
    });

    ngOnInit(): void {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set('');
        this.lotesService.listar().subscribe({
            next: (lotes) => {
                this.lotes.set(lotes);
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudieron cargar los lotes: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    protected pedirCambio(lote: Lote, estado: 'LIBERADO' | 'BLOQUEADO'): void {
        this.motivo = '';
        this.intentoSinMotivo.set(false);
        this.cambio.set({ lote, estado });
    }

    protected cerrarCambio(): void {
        if (!this.guardando()) this.cambio.set(null);
    }

    protected confirmarCambio(): void {
        const c = this.cambio();
        if (!c) return;
        if (!this.motivo.trim()) {
            this.intentoSinMotivo.set(true);
            return;
        }
        this.guardando.set(true);
        this.lotesService.cambiarEstado(c.lote.id, { estado: c.estado, motivo: this.motivo.trim() }).subscribe({
            next: (actualizado) => {
                this.guardando.set(false);
                this.cambio.set(null);
                this.lotes.update((lista) => lista.map((l) => (l.id === actualizado.id ? actualizado : l)));
                this.mensajes.add({
                    severity: 'success',
                    summary: c.estado === 'LIBERADO' ? 'Lote liberado' : 'Lote bloqueado',
                    detail: actualizado.codigo
                });
            },
            error: (err: HttpErrorResponse) => {
                this.guardando.set(false);
                this.mensajes.add({ severity: 'error', summary: 'No se pudo cambiar el estado', detail: mensajeDeError(err) });
            }
        });
    }

    protected verHistorial(lote: Lote): void {
        this.loteHistorial.set(lote);
        this.historial.set([]);
        this.cargandoHistorial.set(true);
        this.lotesService.historialEstados(lote.id).subscribe({
            next: (historial) => {
                this.historial.set(historial);
                this.cargandoHistorial.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.cargandoHistorial.set(false);
                this.loteHistorial.set(null);
                this.mensajes.add({ severity: 'error', summary: 'No se pudo cargar el historial', detail: mensajeDeError(err) });
            }
        });
    }
}
