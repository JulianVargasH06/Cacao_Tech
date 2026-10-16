import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TableModule } from 'primeng/table';

import { Lote } from '../../../core/models/lote.model';
import { LotesService } from '../../../core/services/lotes.service';
import { PageHeader } from '../../../shared/page-header/page-header';

/** Existencias por lote (HU08): cuánto se recibió, cuánto se ha consumido y cuánto queda. */
@Component({
    selector: 'app-inventario',
    imports: [PageHeader, CardModule, TableModule, DecimalPipe, DatePipe],
    template: `
    <section class="page">
      <app-page-header
        titulo="Inventario"
        descripcion="Existencias de cacao disponibles por lote"
        historia="HU08"
        rol="Operador"
      />

      <p-card>
        <p-table [value]="lotes()" [loading]="cargando()" [tableStyle]="{ 'min-width': '50rem' }">
          <ng-template #header>
            <tr>
              <th>Código</th>
              <th>Productor</th>
              <th>Origen</th>
              <th>Recepción</th>
              <th class="num">Recibida</th>
              <th class="num">Consumida</th>
              <th class="num">Disponible</th>
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
            </tr>
          </ng-template>

          <ng-template #emptymessage>
            <tr>
              <td colspan="7" class="vacio">Aún no hay lotes registrados</td>
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
    :host ::ng-deep .p-datatable-tbody > tr {
      background: transparent;
    }
  `
})
export class Inventario implements OnInit {
    private readonly lotesService = inject(LotesService);

    protected readonly lotes = signal<Lote[]>([]);
    protected readonly cargando = signal(true);

    ngOnInit(): void {
        this.lotesService.listar().subscribe((lotes) => {
            this.lotes.set(lotes);
            this.cargando.set(false);
        });
    }

    /** Lo que ya se usó en transformaciones. El backend no lo guarda: se deduce. */
    protected consumida(lote: Lote): number {
        return lote.cantidadRecibida - lote.cantidadDisponible;
    }
}