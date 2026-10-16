import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { FiltroAuditoria, RegistroAuditoria } from '../../../core/models/auditoria.model';
import { EstadoLote, FiltroLotes, TipoLote } from '../../../core/models/lote.model';
import { AuditoriaService } from '../../../core/services/auditoria.service';
import { LotesService } from '../../../core/services/lotes.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { CambioCampo, cambiosDeAuditoria } from '../../../shared/utils/auditoria-cambios';
import { descargarArchivo, nombreConFecha } from '../../../shared/utils/descargas';
import { mensajeDeError } from '../../../shared/utils/errores-api';
import { aFechaIso, finDelDia } from '../../../shared/utils/fechas';

type FilaAuditoria = RegistroAuditoria & { cambios: CambioCampo[] };

/** Auditoría de cambios (HU10) y exportación filtrada de lotes en CSV (HU11). */
@Component({
    selector: 'app-auditoria',
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
        DatePickerModule,
        MessageModule
    ],
    template: `
    <section class="page">
      <app-page-header
        titulo="Auditoría y exportación"
        descripcion="Quién cambió qué y cuándo, y descarga filtrada de los lotes en CSV"
        historia="HU10 · HU11"
        rol="Administrador"
      />

      <!-- HU11: filtrar y exportar lotes -->
      <p-card header="Exportar lotes (CSV)">
        <div class="filtros">
          <input pInputText [(ngModel)]="filtroLotes.codigo" placeholder="Código contiene…" aria-label="Código" />
          <p-select [options]="estados" [(ngModel)]="filtroLotes.estado" optionLabel="etiqueta" optionValue="valor"
            placeholder="Todos los estados" [showClear]="true" />
          <p-select [options]="tipos" [(ngModel)]="filtroLotes.tipoLote" optionLabel="etiqueta" optionValue="valor"
            placeholder="Todos los tipos" [showClear]="true" />
          <p-datepicker [(ngModel)]="lotesDesde" placeholder="Registrados desde" dateFormat="dd/mm/yy" [maxDate]="hoy" [showClear]="true" />
          <p-datepicker [(ngModel)]="lotesHasta" placeholder="Hasta" dateFormat="dd/mm/yy" [maxDate]="hoy" [showClear]="true" />
        </div>
        <div class="acciones">
          @if (coincidencias() !== null) {
            <span class="resultado">{{ coincidencias() }} lotes coinciden con el filtro</span>
          }
          <p-button label="Ver cuántos" icon="pi pi-filter" severity="secondary" [outlined]="true" (onClick)="contarLotes()" />
          <p-button label="Descargar CSV" icon="pi pi-download" [loading]="exportando()" (onClick)="exportar()" />
        </div>
        @if (errorExportar()) {
          <p-message severity="error" [text]="errorExportar()" />
        }
      </p-card>

      <!-- HU10: auditoría con filtros -->
      <p-card header="Cambios registrados">
        <div class="filtros">
          <p-select [options]="entidades" [(ngModel)]="filtroAuditoria.entidad" placeholder="Todas las entidades" [showClear]="true" />
          <p-select [options]="acciones" [(ngModel)]="filtroAuditoria.accion" placeholder="Todas las acciones" [showClear]="true" />
          <input pInputText [(ngModel)]="filtroAuditoria.usuario" placeholder="Correo del usuario" aria-label="Usuario" />
          <p-datepicker [(ngModel)]="auditoriaDesde" placeholder="Desde" dateFormat="dd/mm/yy" [maxDate]="hoy" [showClear]="true" />
          <p-datepicker [(ngModel)]="auditoriaHasta" placeholder="Hasta" dateFormat="dd/mm/yy" [maxDate]="hoy" [showClear]="true" />
        </div>
        <div class="acciones">
          <p-button label="Limpiar" severity="secondary" [text]="true" (onClick)="limpiarAuditoria()" />
          <p-button label="Buscar" icon="pi pi-search" [loading]="cargando()" (onClick)="buscarAuditoria()" />
        </div>

        @if (error()) {
          <p-message severity="error" [text]="error()" />
        }

        <p-table [value]="registros()" [loading]="cargando()" [paginator]="true" [rows]="15" [tableStyle]="{ 'min-width': '52rem' }">
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
              <td>{{ r.usuario }}<small class="correo">{{ r.correoUsuario }}</small></td>
              <td><p-tag [value]="r.accion" [severity]="severidadAccion(r.accion)" [rounded]="true" /></td>
              <td class="sin-salto">{{ r.entidad }} #{{ r.entidadId }}</td>
              <td>
                <ul class="cambios">
                  @for (c of r.cambios; track $index) {
                    <li>
                      @if (c.campo) {
                        <span class="campo">{{ c.campo }}:</span>
                      }
                      @if (c.antes !== undefined) {
                        <span class="anterior">{{ c.antes }}</span> →
                      }
                      {{ c.despues ?? '—' }}
                    </li>
                  }
                </ul>
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr><td colspan="5" class="vacio">Ningún cambio coincide con el filtro</td></tr>
          </ng-template>
        </p-table>
      </p-card>
    </section>
  `,
    styles: `
    .filtros {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
      gap: 0.75rem;
    }
    .acciones {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin: 1rem 0;
    }
    .resultado {
      margin-right: auto;
      color: var(--p-primary-700);
      font-weight: 500;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .correo {
      display: block;
      color: var(--p-text-muted-color);
    }
    .cambios {
      margin: 0;
      padding: 0;
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      font-size: 0.875rem;
    }
    .campo {
      color: var(--p-text-muted-color);
    }
    .anterior {
      color: var(--p-text-muted-color);
      text-decoration: line-through;
    }
    .vacio {
      text-align: center;
      padding: 1.5rem;
      color: var(--p-text-muted-color);
    }
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr,
    :host ::ng-deep .p-paginator {
      background: transparent;
    }
  `
})
export class Auditoria implements OnInit {
    private readonly auditoriaService = inject(AuditoriaService);
    private readonly lotesService = inject(LotesService);

    protected readonly hoy = finDelDia();
    protected readonly estados: { valor: EstadoLote; etiqueta: string }[] = [
        { valor: 'PENDIENTE', etiqueta: 'Pendiente' },
        { valor: 'LIBERADO', etiqueta: 'Liberado' },
        { valor: 'BLOQUEADO', etiqueta: 'Bloqueado' }
    ];
    protected readonly tipos: { valor: TipoLote; etiqueta: string }[] = [
        { valor: 'RECEPCION', etiqueta: 'Recepción' },
        { valor: 'RESULTADO_TRANSFORMACION', etiqueta: 'Transformado' }
    ];
    protected readonly entidades = ['Lote', 'Transformacion', 'Alerta', 'Incidencia', 'Costo', 'Usuario'];
    protected readonly acciones = ['CREAR', 'CONSUMO', 'CAMBIAR_ESTADO', 'RESOLVER', 'ACTUALIZAR'];

    // HU11
    protected filtroLotes: FiltroLotes = {};
    protected lotesDesde: Date | null = null;
    protected lotesHasta: Date | null = null;
    protected readonly coincidencias = signal<number | null>(null);
    protected readonly exportando = signal(false);
    protected readonly errorExportar = signal('');

    // HU10
    protected filtroAuditoria: FiltroAuditoria = {};
    protected auditoriaDesde: Date | null = null;
    protected auditoriaHasta: Date | null = null;
    protected readonly registros = signal<FilaAuditoria[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');

    ngOnInit(): void {
        this.buscarAuditoria();
    }

    protected severidadAccion(accion: string): 'success' | 'info' | 'warn' | 'secondary' {
        if (accion === 'CREAR') return 'success';
        if (accion === 'CONSUMO') return 'info';
        if (accion === 'CAMBIAR_ESTADO' || accion === 'ACTUALIZAR') return 'warn';
        return 'secondary';
    }

    // ─── HU11: exportación ───────────────────────────────────────────────────────

    protected contarLotes(): void {
        this.errorExportar.set('');
        this.lotesService.listar(this.filtroDeLotes()).subscribe({
            next: (lotes) => this.coincidencias.set(lotes.length),
            error: (err: HttpErrorResponse) => this.errorExportar.set(mensajeDeError(err))
        });
    }

    protected exportar(): void {
        this.exportando.set(true);
        this.errorExportar.set('');
        this.lotesService.exportarCsv(this.filtroDeLotes()).subscribe({
            next: (archivo) => {
                descargarArchivo(archivo, nombreConFecha('lotes', 'csv'));
                this.exportando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.errorExportar.set('No se pudo exportar: ' + mensajeDeError(err));
                this.exportando.set(false);
            }
        });
    }

    private filtroDeLotes(): FiltroLotes {
        return {
            ...this.filtroLotes,
            codigo: this.filtroLotes.codigo?.trim() || undefined,
            desde: this.lotesDesde ? aFechaIso(this.lotesDesde) : undefined,
            hasta: this.lotesHasta ? aFechaIso(this.lotesHasta) : undefined
        };
    }

    // ─── HU10: auditoría ─────────────────────────────────────────────────────────

    protected buscarAuditoria(): void {
        this.cargando.set(true);
        this.error.set('');
        const filtro: FiltroAuditoria = {
            ...this.filtroAuditoria,
            usuario: this.filtroAuditoria.usuario?.trim() || undefined,
            desde: this.auditoriaDesde ? aFechaIso(this.auditoriaDesde) : undefined,
            hasta: this.auditoriaHasta ? aFechaIso(this.auditoriaHasta) : undefined
        };
        this.auditoriaService.listar(filtro).subscribe({
            next: (registros) => {
                this.registros.set(registros.map((r) => ({ ...r, cambios: cambiosDeAuditoria(r) })));
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudo cargar la auditoría: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    protected limpiarAuditoria(): void {
        this.filtroAuditoria = {};
        this.auditoriaDesde = null;
        this.auditoriaHasta = null;
        this.buscarAuditoria();
    }
}
