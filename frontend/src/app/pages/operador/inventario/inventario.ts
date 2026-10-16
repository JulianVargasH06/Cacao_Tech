import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';

import { Lote } from '../../../core/models/lote.model';
import { LotesService } from '../../../core/services/lotes.service';
import { PageHeader } from '../../../shared/page-header/page-header';

/** Existencias por lote (HU08): cuánto se recibió, cuánto se ha consumido y cuánto queda. */
@Component({
    selector: 'app-inventario',
    imports: [PageHeader, CardModule, TableModule, DecimalPipe,
        DatePipe, FormsModule, InputTextModule, TagModule, MessageModule
        , ButtonModule],
    template: `
    <section class="page">
      <app-page-header
        titulo="Inventario"
        descripcion="Existencias de cacao disponibles por lote"
        historia="HU08"
        rol="Operador"
      />

        @if (error()) {
            <p-message severity="error">
            <div class="error-carga">
                <span>{{ error() }}</span>
                <p-button label="Reintentar" icon="pi pi-refresh" size="small" [text]="true" (onClick)="cargar()" />
            </div>
            </p-message>
        }

      <p-card>
        <p-table [value]="lotesFiltrados()" [loading]="cargando()" [tableStyle]="{ 'min-width': '50rem' }">
          
        <ng-template #caption>
            <div class="barra">
              <input
                pInputText
                type="search"
                placeholder="Buscar por código o productor"
                [ngModel]="filtro()"
                (ngModelChange)="filtro.set($event)"
              />
              <span class="total">{{ lotesFiltrados().length }} de {{ lotes().length }} lotes</span>
            </div>
          </ng-template>
        <ng-template #header>
            <tr>
              <th>Código</th>
              <th>Productor</th>
              <th>Origen</th>
              <th>Recepción</th>
              <th class="num">Recibida</th>
              <th class="num">Consumida</th>
              <th class="num">Disponible</th>
              <th>Estado</th>
            </tr>
          </ng-template>

          <ng-template #body let-lote>
            <tr>
              <td><strong>{{ lote.codigo }}</strong></td>
              <td>{{ lote.productor }}</td>
              <td>{{ lote.origen }}</td>
              <td>{{ lote.fechaRecepcion | date: 'dd/MM/yyyy' }}</td>
              <td class="num">{{ lote.cantidadRecibida | number: '1.0-2' }} {{ lote.unidad }}</td>
              <td class="num">{{ consumida(lote) | number: '1.0-2' }} {{ lote.unidad }}</td>
              <td class="num">
                <strong>{{ lote.cantidadDisponible | number: '1.0-2' }} {{ lote.unidad }}</strong>
              </td>
              <td>
                @let e = estado(lote);
                <p-tag [value]="e.texto" [severity]="e.severidad" [rounded]="true" />
              </td>
            </tr>
          </ng-template>

          <ng-template #emptymessage>
            <tr>
              <td colspan="8" class="vacio">
                @if (filtro()) {
                  Ningún lote coincide con "{{ filtro() }}"
                } @else {
                  Aún no hay lotes registrados
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      </p-card>
    </section>
  `,
    styles: `
    .num {
      text-align: right;
      white-space: nowrap;
    }
    .vacio {
      text-align: center;
      padding: 2rem;
      color: var(--p-text-muted-color);
    }
    /* La tabla deja ver el cristal de la tarjeta en vez de pintar su propio fondo blanco */
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-header,
    :host ::ng-deep .p-datatable-tbody > tr {
      background: transparent;
    }

    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
    }
    .barra input {
      width: min(100%, 20rem);
    }
    .total {
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
    }
    .error-carga {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
  `
})
export class Inventario implements OnInit {
    private readonly lotesService = inject(LotesService);

    protected readonly lotes = signal<Lote[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly filtro = signal('');

    /** Se recalcula sola cada vez que cambian `lotes` o `filtro`. */
    protected readonly lotesFiltrados = computed(() => {
        const texto = this.filtro().trim().toLowerCase();
        if (!texto) return this.lotes();
        return this.lotes().filter(
            (l) => l.codigo.toLowerCase().includes(texto) || l.productor.toLowerCase().includes(texto)
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
            error: () => {
                this.error.set('No se pudo cargar el inventario');
                this.cargando.set(false);
            }
        });
    }

    /** Lo que ya se usó en transformaciones. El backend no lo guarda: se deduce. */
    protected consumida(lote: Lote): number {
        return lote.cantidadRecibida - lote.cantidadDisponible;
    }

    /** Etiqueta según cuánto queda: avisa antes de que alguien intente consumir de un lote vacío. */
    protected estado(lote: Lote): { texto: string; severidad: 'success' | 'warn' | 'danger' } {
        if (lote.cantidadDisponible <= 0) return { texto: 'Agotado', severidad: 'danger' };
        if (lote.cantidadDisponible / lote.cantidadRecibida < 0.2) return { texto: 'Bajo', severidad: 'warn' };
        return { texto: 'Disponible', severidad: 'success' };
    }
}