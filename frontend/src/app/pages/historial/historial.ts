import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { RegistroAuditoria } from '../../core/models/auditoria.model';
import { Lote } from '../../core/models/lote.model';
import { ETAPAS, Transformacion } from '../../core/models/transformacion.models';
import { AuditoriaService } from '../../core/services/auditoria.service';
import { AuthService } from '../../core/services/auth.service';
import { LotesService } from '../../core/services/lotes.service';
import { TransformacionesService } from '../../core/services/transformaciones.service';
import { PageHeader } from '../../shared/page-header/page-header';

type TipoOperacion = 'RECEPCION' | 'TRANSFORMACION';
type Filtro = 'TODAS' | TipoOperacion;

/** Una fila del historial: une recepciones y transformaciones en un mismo formato. */
interface Operacion {
    clave: string;
    tipo: TipoOperacion;
    fecha: string; // ISO, para ordenar
    conHora: boolean; // las recepciones solo tienen fecha
    codigo: string;
    detalle: string;
    nota?: string;
    resultado: string;
    responsable: string;
}

/**
 * Historial de operaciones (apoya HU02, HU03 y HU10). Lo ven ambos roles;
 * los cambios de auditoría solo se muestran al administrador.
 */
@Component({
    selector: 'app-historial',
    imports: [
        FormsModule,
        DatePipe,
        PageHeader,
        CardModule,
        TableModule,
        TagModule,
        SelectButtonModule,
        InputTextModule,
        MessageModule,
        ButtonModule
    ],
    template: `
    <section class="page">
      <app-page-header
        titulo="Historial de operaciones"
        descripcion="Recepciones y transformaciones registradas, con sus resultados"
        historia="HU02 · HU03 · HU10"
        [rol]="rolTexto()"
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
          [value]="operacionesFiltradas()"
          [loading]="cargando()"
          [paginator]="true"
          [rows]="10"
          [tableStyle]="{ 'min-width': '56rem' }"
        >
          <ng-template #caption>
            <div class="barra">
              <p-selectbutton
                [options]="filtros"
                [ngModel]="filtro()"
                (ngModelChange)="filtro.set($event)"
                optionLabel="etiqueta"
                optionValue="valor"
                [allowEmpty]="false"
              />
              <input
                pInputText
                type="search"
                placeholder="Buscar por código, lote o responsable"
                [ngModel]="busqueda()"
                (ngModelChange)="busqueda.set($event)"
              />
              <span class="total">{{ operacionesFiltradas().length }} de {{ operaciones().length }} operaciones</span>
            </div>
          </ng-template>

          <ng-template #header>
            <tr>
              <th>Fecha</th>
              <th>Tipo</th>
              <th>Código</th>
              <th>Detalle</th>
              <th>Resultado</th>
              <th>Responsable</th>
            </tr>
          </ng-template>

          <ng-template #body let-op>
            <tr>
              <td class="sin-salto">{{ op.fecha | date: (op.conHora ? 'dd/MM/yyyy HH:mm' : 'dd/MM/yyyy') }}</td>
              <td>
                <p-tag
                  [value]="op.tipo === 'RECEPCION' ? 'Recepción' : 'Transformación'"
                  [severity]="op.tipo === 'RECEPCION' ? 'success' : 'info'"
                  [rounded]="true"
                />
              </td>
              <td class="sin-salto"><strong>{{ op.codigo }}</strong></td>
              <td>
                {{ op.detalle }}
                @if (op.nota) {
                  <small class="nota">{{ op.nota }}</small>
                }
              </td>
              <td>{{ op.resultado }}</td>
              <td>{{ op.responsable }}</td>
            </tr>
          </ng-template>

          <ng-template #emptymessage>
            <tr>
              <td colspan="6" class="vacio">
                @if (busqueda() || filtro() !== 'TODAS') {
                  Ninguna operación coincide con el filtro
                } @else {
                  Aún no hay operaciones registradas
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      </p-card>

      <!-- HU10: los cambios de auditoría dependen de los permisos del usuario -->
      @if (esAdmin()) {
        <p-card header="Cambios de auditoría">
          <p-table
            [value]="auditoria()"
            [loading]="cargando()"
            [paginator]="true"
            [rows]="10"
            [tableStyle]="{ 'min-width': '50rem' }"
          >
            <ng-template #header>
              <tr>
                <th>Fecha</th>
                <th>Usuario</th>
                <th>Acción</th>
                <th>Entidad</th>
                <th>Cambio</th>
              </tr>
            </ng-template>
            <ng-template #body let-r>
              <tr>
                <td class="sin-salto">{{ r.fecha | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ r.usuario }}</td>
                <td><p-tag [value]="r.accion" [severity]="severidadAccion(r.accion)" [rounded]="true" /></td>
                <td>{{ r.entidad }} #{{ r.entidadId }}</td>
                <td>
                  @if (r.valoresAnteriores) {
                    <span class="anterior">{{ r.valoresAnteriores }}</span> →
                  }
                  {{ r.valoresNuevos }}
                </td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr>
                <td colspan="5" class="vacio">No hay cambios registrados</td>
              </tr>
            </ng-template>
          </p-table>
        </p-card>
      } @else {
        <p-message severity="secondary" icon="pi pi-lock">
          Los cambios de auditoría solo los puede consultar el administrador.
        </p-message>
      }
    </section>
  `,
    styles: `
    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem 1rem;
    }
    .barra input {
      width: min(100%, 20rem);
    }
    .total {
      margin-left: auto;
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .nota {
      display: block;
      margin-top: 0.2rem;
      color: var(--p-text-muted-color);
    }
    .anterior {
      color: var(--p-text-muted-color);
      text-decoration: line-through;
    }
    .vacio {
      text-align: center;
      padding: 2rem;
      color: var(--p-text-muted-color);
    }
    .mensaje-accion {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    /* La tabla deja ver el cristal de la tarjeta en vez de pintar su propio fondo blanco */
    :host ::ng-deep .p-datatable-header,
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr,
    :host ::ng-deep .p-paginator {
      background: transparent;
    }
  `
})
export class Historial implements OnInit {
    private readonly auth = inject(AuthService);
    private readonly lotesService = inject(LotesService);
    private readonly transformacionesService = inject(TransformacionesService);
    private readonly auditoriaService = inject(AuditoriaService);

    protected readonly filtros: { valor: Filtro; etiqueta: string }[] = [
        { valor: 'TODAS', etiqueta: 'Todas' },
        { valor: 'RECEPCION', etiqueta: 'Recepciones' },
        { valor: 'TRANSFORMACION', etiqueta: 'Transformaciones' }
    ];

    protected readonly operaciones = signal<Operacion[]>([]);
    protected readonly auditoria = signal<RegistroAuditoria[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly filtro = signal<Filtro>('TODAS');
    protected readonly busqueda = signal('');

    protected readonly esAdmin = computed(() => this.auth.rol() === 'ADMINISTRADOR');
    protected readonly rolTexto = computed(() => (this.esAdmin() ? 'Administrador' : 'Operador'));

    protected readonly operacionesFiltradas = computed(() => {
        const tipo = this.filtro();
        const texto = this.busqueda().trim().toLowerCase();
        return this.operaciones().filter(
            (op) =>
                (tipo === 'TODAS' || op.tipo === tipo) &&
                (!texto || `${op.codigo} ${op.detalle} ${op.responsable}`.toLowerCase().includes(texto))
        );
    });

    ngOnInit(): void {
        this.cargar();
    }

    /** Pide todo en paralelo y espera a que lleguen las tres respuestas (forkJoin). */
    protected cargar(): void {
        this.cargando.set(true);
        this.error.set('');
        forkJoin({
            lotes: this.lotesService.listar(),
            transformaciones: this.transformacionesService.listar(),
            // El operador no tiene permiso: ni siquiera se pide.
            auditoria: this.esAdmin() ? this.auditoriaService.listar() : of([])
        }).subscribe({
            next: ({ lotes, transformaciones, auditoria }) => {
                this.operaciones.set(this.armarOperaciones(lotes, transformaciones));
                this.auditoria.set(auditoria);
                this.cargando.set(false);
            },
            error: () => {
                this.error.set('No se pudo cargar el historial');
                this.cargando.set(false);
            }
        });
    }

    protected severidadAccion(accion: string): 'success' | 'warn' | 'secondary' {
        if (accion === 'CREAR') return 'success';
        if (accion === 'CAMBIAR_ESTADO') return 'warn';
        return 'secondary';
    }

    /** Convierte lotes y transformaciones en filas del historial, de la más reciente a la más antigua. */
    private armarOperaciones(lotes: Lote[], transformaciones: Transformacion[]): Operacion[] {
        const codigoPorId = new Map(lotes.map((l) => [l.id, l.codigo]));

        // Los lotes que salieron de una transformación ya aparecen como resultado de esa transformación.
        const recepciones: Operacion[] = lotes
            .filter((l) => l.tipoLote === 'RECEPCION')
            .map((l) => ({
                clave: `lote-${l.id}`,
                tipo: 'RECEPCION',
                fecha: `${l.fechaRegistro}T00:00:00`,
                conHora: false,
                codigo: l.codigo,
                detalle: `${l.productor} · ${l.origen}`,
                resultado: `${kg(l.cantidadInicial)} kg recibidos`,
                responsable: '—' // el backend no guarda quién recibió el lote
            }));

        const procesos: Operacion[] = transformaciones.map((t) => {
            const entra = t.entradas.reduce((suma, e) => suma + e.cantidadKg, 0);
            const sale = t.salidas.reduce((suma, s) => suma + s.cantidadKg, 0);
            const merma = entra - sale;
            const desde = t.entradas.map((e) => codigoPorId.get(e.loteId) ?? `#${e.loteId}`).join(', ');
            const hacia = t.salidas.map((s) => s.codigo).join(', ');
            const etapa = ETAPAS.find((e) => e.valor === t.tipo)?.etiqueta ?? t.tipo;
            return {
                clave: `tr-${t.id}`,
                tipo: 'TRANSFORMACION',
                fecha: t.fecha,
                conHora: true,
                codigo: t.codigo,
                detalle: `${etapa} · ${desde} → ${hacia}`,
                nota: t.observaciones,
                resultado: `${kg(entra)} → ${kg(sale)} kg · merma ${kg(merma)} kg (${porcentaje(merma / entra)})`,
                responsable: t.responsable
            };
        });

        return [...recepciones, ...procesos].sort((a, b) => b.fecha.localeCompare(a.fecha));
    }
}

/** 1234.5 → "1.234,5" (formato colombiano). */
function kg(valor: number): string {
    return valor.toLocaleString('es-CO', { maximumFractionDigits: 2 });
}

/** 0.1 → "10,0 %" */
function porcentaje(fraccion: number): string {
    return `${(fraccion * 100).toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} %`;
}
