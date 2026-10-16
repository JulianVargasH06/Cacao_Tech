import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

import { Alerta, EstadoAlerta } from '../../../core/models/alerta.model';
import { Margen } from '../../../core/models/lote.model';
import { CostosTransformacion, ETAPAS, Transformacion } from '../../../core/models/transformacion.models';
import { AlertasService } from '../../../core/services/alertas.service';
import { LotesService } from '../../../core/services/lotes.service';
import { TransformacionesService } from '../../../core/services/transformaciones.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { mensajeDeError } from '../../../shared/utils/errores-api';
import { etiquetaAlerta, formatoCop, formatoKg, formatoPorcentaje } from '../../../shared/utils/formatos';

/** Mismo umbral que MermaAlertaEvaluador del backend: una merma mayor genera alerta y bloqueo. */
const UMBRAL_MERMA = 15;

/** Fila de la tabla: la transformación con sus totales ya calculados. */
interface FilaMerma {
    t: Transformacion;
    etapa: string;
    entradaKg: number;
    salidaKg: number;
    mermaKg: number;
    mermaPorcentaje: number;
    costoTotal: number;
    costoPorKg: number | null;
}

/**
 * Análisis de mermas y costos (HU04, HU05): alertas explicables, merma de cada transformación
 * frente al umbral, costos de proceso, costo unitario y margen sobre venta.
 */
@Component({
    selector: 'app-mermas-costos',
    imports: [
        FormsModule,
        DatePipe,
        PageHeader,
        CardModule,
        TableModule,
        TagModule,
        ButtonModule,
        SelectButtonModule,
        DialogModule,
        SelectModule,
        InputTextModule,
        InputNumberModule,
        MessageModule,
        ToastModule
    ],
    providers: [MessageService],
    template: `
    <section class="page">
      <p-toast />
      <app-page-header
        titulo="Mermas y costos"
        descripcion="Merma de cada transformación frente al umbral del 15 %, costos de proceso y margen sobre venta"
        historia="HU04 · HU05"
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

      <!-- 1. Alertas explicables (HU04) -->
      <p-card>
        <ng-template #title>
          <div class="titulo-tarjeta">
            <span>Alertas</span>
            <p-selectbutton
              [options]="filtrosAlerta"
              [ngModel]="estadoAlertas()"
              (ngModelChange)="estadoAlertas.set($event)"
              optionLabel="etiqueta"
              optionValue="valor"
              [allowEmpty]="false"
              size="small"
            />
          </div>
        </ng-template>
        @if (!cargando() && alertasVisibles().length === 0) {
          <p class="vacio">
            <i class="pi pi-check-circle"></i>
            {{ estadoAlertas() === 'ACTIVA' ? 'No hay alertas activas' : 'No hay alertas resueltas' }}
          </p>
        }
        <ul class="alertas">
          @for (a of alertasVisibles(); track a.id) {
            <li>
              <p-tag
                [value]="etiquetaAlerta(a.tipo)"
                [severity]="a.estado === 'ACTIVA' ? 'danger' : 'secondary'"
                [rounded]="true"
              />
              <div class="alerta-texto">
                <p>{{ a.descripcion }}</p>
                <small>
                  {{ a.codigoTransformacion }} · lote {{ a.codigoLote }} · {{ a.creadaEn | date: 'dd/MM/yyyy HH:mm' }}
                  @if (a.resueltaPor) {
                    · resuelta por {{ a.resueltaPor }} el {{ a.resueltaEn | date: 'dd/MM/yyyy HH:mm' }}
                  }
                </small>
              </div>
              @if (a.estado === 'ACTIVA') {
                <p-button label="Resolver" icon="pi pi-check" size="small" [outlined]="true"
                  [loading]="resolviendo() === a.id" (onClick)="resolver(a)" />
              }
            </li>
          }
        </ul>
      </p-card>

      <!-- 2. Merma y costo de cada transformación -->
      <p-card header="Transformaciones">
        <p-table [value]="filas()" [loading]="cargando()" [paginator]="true" [rows]="10" [tableStyle]="{ 'min-width': '60rem' }">
          <ng-template #header>
            <tr>
              <th>Código</th>
              <th>Etapa</th>
              <th>Fecha</th>
              <th class="num">Entrada</th>
              <th class="num">Salida</th>
              <th class="num">Merma</th>
              <th class="num">Costo total</th>
              <th class="num">Costo / kg</th>
              <th></th>
            </tr>
          </ng-template>
          <ng-template #body let-f>
            <tr>
              <td class="sin-salto"><strong>{{ f.t.codigo }}</strong></td>
              <td>{{ f.etapa }}</td>
              <td class="sin-salto">{{ f.t.fecha | date: 'dd/MM/yyyy' }}</td>
              <td class="num">{{ kg(f.entradaKg) }} kg</td>
              <td class="num">{{ kg(f.salidaKg) }} kg</td>
              <td class="num">
                <p-tag [value]="porcentaje(f.mermaPorcentaje)" [severity]="f.mermaPorcentaje > umbral ? 'danger' : 'success'" [rounded]="true" />
                <small class="detalle-merma">{{ kg(f.mermaKg) }} kg</small>
              </td>
              <td class="num">{{ f.costoTotal > 0 ? cop(f.costoTotal) : '—' }}</td>
              <td class="num">{{ f.costoPorKg !== null ? cop(f.costoPorKg) : '—' }}</td>
              <td>
                <p-button label="Costos" icon="pi pi-wallet" size="small" severity="secondary" [text]="true" (onClick)="abrirCostos(f.t)" />
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr><td colspan="9" class="vacio">Aún no hay transformaciones registradas</td></tr>
          </ng-template>
        </p-table>
      </p-card>

      <!-- 3. Costos de una transformación y margen de sus salidas (HU05) -->
      <p-dialog
        [header]="'Costos de ' + (detalle()?.codigo ?? '')"
        [visible]="!!seleccionada()"
        (visibleChange)="!$event && cerrarCostos()"
        [modal]="true"
        [style]="{ width: 'min(46rem, 96vw)' }"
      >
        @if (cargandoDetalle()) {
          <p class="vacio">Cargando…</p>
        } @else if (detalle(); as d) {
          <h4>Costos registrados · total {{ cop(d.costoTotalCop) }}</h4>
          @if (d.costos.length === 0) {
            <p class="vacio">Aún no hay costos. Agrega el primero abajo.</p>
          } @else {
            <table class="tabla-simple">
              <tr><th>Categoría</th><th>Descripción</th><th class="num">Valor</th><th>Registró</th></tr>
              @for (c of d.costos; track c.id) {
                <tr><td>{{ c.categoria }}</td><td>{{ c.descripcion }}</td><td class="num">{{ cop(c.valorCop) }}</td><td>{{ c.usuario }}</td></tr>
              }
            </table>
          }

          <form class="nuevo-costo" (ngSubmit)="agregarCosto()">
            <p-select [options]="categorias" [(ngModel)]="nuevoCosto.categoria" name="categoria" placeholder="Categoría" />
            <input pInputText [(ngModel)]="nuevoCosto.descripcion" name="descripcion" placeholder="Descripción" />
            <p-inputnumber [(ngModel)]="nuevoCosto.valorCop" name="valor" mode="currency" currency="COP" locale="es-CO"
              [maxFractionDigits]="0" placeholder="Valor" />
            <p-button type="submit" label="Agregar" icon="pi pi-plus" [loading]="guardandoCosto()" />
          </form>

          <h4>Costo por lote resultante y margen sobre venta</h4>
          <p class="ayuda">El costo total se reparte entre los lotes resultantes según sus kg.</p>
          <table class="tabla-simple">
            <tr><th>Lote</th><th class="num">Kg</th><th class="num">Costo asignado</th><th class="num">Costo / kg</th><th>Precio de venta / kg</th></tr>
            @for (s of d.salidas; track s.loteId) {
              <tr>
                <td><strong>{{ s.codigo }}</strong></td>
                <td class="num">{{ kg(s.cantidadKg) }}</td>
                <td class="num">{{ cop(s.costoAsignadoCop) }}</td>
                <td class="num">{{ cop(s.costoUnitarioCop) }}</td>
                <td>
                  <div class="margen">
                    <p-inputnumber [(ngModel)]="precios[s.loteId]" [name]="'precio' + s.loteId" mode="currency" currency="COP"
                      locale="es-CO" [maxFractionDigits]="0" placeholder="$ por kg" [disabled]="s.costoUnitarioCop == null" />
                    <p-button icon="pi pi-calculator" size="small" [outlined]="true" ariaLabel="Calcular margen"
                      [disabled]="s.costoUnitarioCop == null" (onClick)="calcularMargen(s.loteId)" />
                  </div>
                  @if (margenes()[s.loteId]; as m) {
                    <small [class.negativo]="m.margenPorcentaje < 0">
                      Margen {{ porcentaje(m.margenPorcentaje) }} · {{ cop(m.gananciaPorKgCop) }} por kg
                    </small>
                    <small class="calculo">
                      ({{ cop(m.precioVentaKgCop) }} − {{ cop(m.costoUnitarioCop) }}) ÷ {{ cop(m.precioVentaKgCop) }} × 100
                    </small>
                  }
                </td>
              </tr>
            }
          </table>

          <div class="formulas">
            <strong><i class="pi pi-info-circle"></i> Cómo se calcula</strong>
            <ul>
              <li>Costo / kg = costo total de la transformación ÷ kg resultantes</li>
              <li>Margen sobre venta = (precio de venta / kg − costo / kg) ÷ precio de venta / kg × 100</li>
            </ul>
            <strong>Supuestos</strong>
            <ul>
              <li>Solo cuentan los costos registrados en esta transformación (incluida la materia prima, si se registró como MATERIA_PRIMA).</li>
              <li>Si hay varios lotes resultantes, el costo se reparte en proporción a sus kg.</li>
              <li>La merma no tiene costo propio: su efecto es que el costo se divide entre menos kg.</li>
              <li>Valores en pesos colombianos (COP), sin impuestos.</li>
            </ul>
          </div>
        }
      </p-dialog>
    </section>
  `,
    styles: `
    .titulo-tarjeta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .alertas {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .alertas li {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
    }
    .alerta-texto {
      flex: 1;
    }
    .alerta-texto p {
      margin: 0 0 0.2rem;
    }
    small,
    .ayuda {
      color: var(--p-text-muted-color);
    }
    .vacio {
      text-align: center;
      padding: 1.25rem;
      color: var(--p-text-muted-color);
    }
    .num {
      text-align: right;
      white-space: nowrap;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .detalle-merma {
      display: block;
    }
    h4 {
      margin: 1rem 0 0.5rem;
      color: var(--p-primary-800);
    }
    .tabla-simple {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }
    .tabla-simple th,
    .tabla-simple td {
      padding: 0.45rem 0.5rem;
      border-bottom: 1px solid var(--p-content-border-color);
      text-align: left;
    }
    .tabla-simple .num {
      text-align: right;
    }
    .nuevo-costo {
      display: grid;
      grid-template-columns: 10rem 1fr 9rem auto;
      gap: 0.5rem;
      margin-top: 0.75rem;
    }
    .margen {
      display: flex;
      gap: 0.35rem;
      align-items: center;
    }
    .margen p-inputnumber {
      width: 8.5rem;
    }
    .negativo {
      color: var(--p-red-500);
    }
    .calculo {
      display: block;
      color: var(--p-text-muted-color);
    }
    .formulas {
      margin-top: 1.25rem;
      padding: 0.85rem 1rem;
      border-radius: 1rem;
      background: var(--p-primary-50);
      font-size: 0.875rem;
    }
    .formulas ul {
      margin: 0.35rem 0 0.75rem;
      padding-left: 1.2rem;
    }
    .formulas ul:last-child {
      margin-bottom: 0;
    }
    .mensaje-accion {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    @media (max-width: 640px) {
      .nuevo-costo {
        grid-template-columns: 1fr;
      }
    }
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr,
    :host ::ng-deep .p-paginator {
      background: transparent;
    }
  `
})
export class MermasCostos implements OnInit {
    private readonly transformacionesService = inject(TransformacionesService);
    private readonly alertasService = inject(AlertasService);
    private readonly lotesService = inject(LotesService);
    private readonly mensajes = inject(MessageService);

    protected readonly umbral = UMBRAL_MERMA;
    protected readonly categorias = ['MATERIA_PRIMA', 'MANO_DE_OBRA', 'ENERGIA', 'EMPAQUE', 'TRANSPORTE', 'INSUMOS', 'OTRO'];
    protected readonly filtrosAlerta: { valor: EstadoAlerta; etiqueta: string }[] = [
        { valor: 'ACTIVA', etiqueta: 'Activas' },
        { valor: 'RESUELTA', etiqueta: 'Resueltas' }
    ];

    protected readonly transformaciones = signal<Transformacion[]>([]);
    protected readonly alertas = signal<Alerta[]>([]);
    protected readonly estadoAlertas = signal<EstadoAlerta>('ACTIVA');
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly resolviendo = signal<number | null>(null);

    protected readonly seleccionada = signal<Transformacion | null>(null);
    protected readonly detalle = signal<CostosTransformacion | null>(null);
    protected readonly cargandoDetalle = signal(false);
    protected readonly guardandoCosto = signal(false);
    protected nuevoCosto = { categoria: 'MANO_DE_OBRA', descripcion: '', valorCop: null as number | null };
    protected precios: Record<number, number | null> = {};
    protected readonly margenes = signal<Record<number, Margen>>({});

    protected readonly kg = formatoKg;
    protected readonly etiquetaAlerta = etiquetaAlerta;
    protected readonly cop = formatoCop;
    protected readonly porcentaje = (v: number) => formatoPorcentaje(v);

    protected readonly alertasVisibles = computed(() => this.alertas().filter((a) => a.estado === this.estadoAlertas()));

    protected readonly filas = computed<FilaMerma[]>(() =>
        this.transformaciones().map((t) => {
            const entradaKg = t.entradas.reduce((s, e) => s + e.cantidadKg, 0);
            const salidaKg = t.salidas.reduce((s, x) => s + x.cantidadKg, 0);
            const mermaKg = t.mermaKg ?? entradaKg - salidaKg;
            const costoTotal = t.costoTotalCop ?? 0;
            return {
                t,
                etapa: ETAPAS.find((e) => e.valor === t.tipo)?.etiqueta ?? t.tipo,
                entradaKg,
                salidaKg,
                mermaKg,
                mermaPorcentaje: t.mermaPorcentaje ?? (entradaKg > 0 ? (mermaKg / entradaKg) * 100 : 0),
                costoTotal,
                costoPorKg: costoTotal > 0 && salidaKg > 0 ? costoTotal / salidaKg : null
            };
        })
    );

    ngOnInit(): void {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set('');
        forkJoin({
            transformaciones: this.transformacionesService.listar(),
            alertas: this.alertasService.listar()
        }).subscribe({
            next: ({ transformaciones, alertas }) => {
                this.transformaciones.set(transformaciones);
                this.alertas.set(alertas);
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudo cargar la información: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    protected resolver(alerta: Alerta): void {
        this.resolviendo.set(alerta.id);
        this.alertasService.resolver(alerta.id).subscribe({
            next: (resuelta) => {
                this.resolviendo.set(null);
                this.alertas.update((lista) => lista.map((a) => (a.id === resuelta.id ? resuelta : a)));
                this.mensajes.add({ severity: 'success', summary: 'Alerta resuelta', detail: resuelta.codigoTransformacion ?? '' });
            },
            error: (err: HttpErrorResponse) => {
                this.resolviendo.set(null);
                this.mensajes.add({ severity: 'error', summary: 'No se pudo resolver', detail: mensajeDeError(err) });
            }
        });
    }

    protected abrirCostos(t: Transformacion): void {
        this.seleccionada.set(t);
        this.detalle.set(null);
        this.precios = {};
        this.margenes.set({});
        this.nuevoCosto = { categoria: 'MANO_DE_OBRA', descripcion: '', valorCop: null };
        this.cargandoDetalle.set(true);
        this.transformacionesService.costos(t.id).subscribe({
            next: (detalle) => {
                this.detalle.set(detalle);
                this.cargandoDetalle.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.cargandoDetalle.set(false);
                this.seleccionada.set(null);
                this.mensajes.add({ severity: 'error', summary: 'No se pudieron cargar los costos', detail: mensajeDeError(err) });
            }
        });
    }

    protected cerrarCostos(): void {
        this.seleccionada.set(null);
        this.detalle.set(null);
    }

    protected agregarCosto(): void {
        const t = this.seleccionada();
        const { categoria, descripcion, valorCop } = this.nuevoCosto;
        if (!t || !categoria || !descripcion.trim() || !valorCop || valorCop <= 0) {
            this.mensajes.add({ severity: 'warn', summary: 'Completa el costo', detail: 'Categoría, descripción y un valor mayor a cero' });
            return;
        }
        this.guardandoCosto.set(true);
        this.transformacionesService.registrarCosto(t.id, { categoria, descripcion: descripcion.trim(), valorCop }).subscribe({
            next: (detalle) => {
                this.guardandoCosto.set(false);
                this.detalle.set(detalle);
                this.margenes.set({}); // el costo por kg cambió: los márgenes calculados ya no aplican
                this.nuevoCosto = { categoria, descripcion: '', valorCop: null };
                this.mensajes.add({ severity: 'success', summary: 'Costo agregado', detail: `Total ${formatoCop(detalle.costoTotalCop)}` });
                this.transformacionesService.listar().subscribe((lista) => this.transformaciones.set(lista));
            },
            error: (err: HttpErrorResponse) => {
                this.guardandoCosto.set(false);
                this.mensajes.add({ severity: 'error', summary: 'No se pudo agregar el costo', detail: mensajeDeError(err) });
            }
        });
    }

    protected calcularMargen(loteId: number): void {
        // InputNumber actualiza el valor al salir del campo: por eso se valida aquí y no deshabilitando el botón
        const precio = this.precios[loteId];
        if (!precio || precio <= 0) {
            this.mensajes.add({ severity: 'warn', summary: 'Escribe el precio de venta por kg' });
            return;
        }
        this.lotesService.margen(loteId, precio).subscribe({
            next: (margen) => this.margenes.update((m) => ({ ...m, [loteId]: margen })),
            error: (err: HttpErrorResponse) =>
                this.mensajes.add({ severity: 'error', summary: 'No se pudo calcular el margen', detail: mensajeDeError(err) })
        });
    }
}
