import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { merge } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';

import { Lote } from '../../../core/models/lote.model';
import { ETAPAS, Etapa, TransformacionRequest } from '../../../core/models/transformacion.models';
import { AuthService } from '../../../core/services/auth.service';
import { LotesService } from '../../../core/services/lotes.service';
import { TransformacionesService } from '../../../core/services/transformaciones.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { esCodigoDuplicado, mensajeDeError } from '../../../shared/utils/errores-api';
import { aFechaHoraIso, fechaHoraNoFutura, finDelDia } from '../../../shared/utils/fechas';

/**
 * Registro de transformaciones (HU02): se toma cacao de un lote, pasa por una etapa
 * y lo que sale queda como un lote nuevo. La diferencia entre entrada y salida es la merma.
 */
@Component({
    selector: 'app-transformaciones',
    imports: [
        ReactiveFormsModule,
        RouterLink,
        DecimalPipe,
        PageHeader,
        CardModule,
        SelectModule,
        DatePickerModule,
        InputNumberModule,
        InputTextModule,
        TextareaModule,
        MessageModule,
        ButtonModule,
        ToastModule
    ],
    providers: [MessageService],
    template: `
    <section class="page">
      <p-toast />
      <app-page-header
        titulo="Transformaciones"
        descripcion="Registra qué le pasó al cacao de un lote en cada etapa del proceso"
        historia="HU02"
        rol="Operador"
      />

      @if (error()) {
        <p-message severity="error">
          <div class="mensaje-accion">
            <span>{{ error() }}</span>
            <p-button label="Reintentar" icon="pi pi-refresh" size="small" [text]="true" (onClick)="cargarLotes()" />
          </div>
        </p-message>
      } @else if (!cargando() && opcionesLote().length === 0) {
        <p-message severity="warn">
          <div class="mensaje-accion">
            <span>No hay lotes con cacao disponible para transformar.</span>
            <a routerLink="/operador/recepcion">Registrar un lote</a>
          </div>
        </p-message>
      }

      <p-card>
        <form [formGroup]="form" (ngSubmit)="guardar()" class="formulario">
          <div class="campo ancho-completo">
            <label for="lote">Lote de entrada</label>
            <p-select
              inputId="lote"
              formControlName="loteId"
              [options]="opcionesLote()"
              optionLabel="etiqueta"
              optionValue="id"
              [filter]="true"
              filterBy="etiqueta"
              [loading]="cargando()"
              placeholder="Selecciona el lote del que se toma el cacao"
              emptyMessage="No hay lotes con cacao disponible"
              [fluid]="true"
            />
            @if (loteSeleccionado(); as lote) {
              <small class="ayuda">
                Disponible: <strong>{{ lote.cantidadDisponible | number: '1.0-2' }} {{ unidad() }}</strong>
                · {{ lote.productor }}
              </small>
            }
            @if (tieneError('loteId', 'required')) {
              <small class="error">Selecciona un lote</small>
            }
          </div>

          <div class="campo">
            <label for="tipo">Etapa</label>
            <p-select
              inputId="tipo"
              formControlName="tipo"
              [options]="etapas"
              optionLabel="etiqueta"
              optionValue="valor"
              placeholder="Selecciona la etapa"
              [fluid]="true"
            />
            @if (tieneError('tipo', 'required')) {
              <small class="error">Selecciona una etapa</small>
            }
          </div>

          <div class="campo">
            <label for="fecha">Fecha y hora</label>
            <p-datepicker
              inputId="fecha"
              formControlName="fecha"
              [maxDate]="hoy"
              [showTime]="true"
              hourFormat="24"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              [fluid]="true"
            />
            @if (tieneError('fecha', 'required')) {
              <small class="error">La fecha es obligatoria</small>
            }
            @if (tieneError('fecha', 'fechaFutura')) {
              <small class="error">La fecha y hora no pueden ser posteriores a este momento</small>
            }
          </div>

          <div class="campo">
            <label for="entrada">Cantidad que entra</label>
            <p-inputnumber
              inputId="entrada"
              formControlName="entrada"
              [minFractionDigits]="0"
              [maxFractionDigits]="2"
              [suffix]="' ' + unidad()"
              locale="es-CO"
              placeholder="0"
              [fluid]="true"
            />
            @if (tieneError('entrada', 'required')) {
              <small class="error">La cantidad de entrada es obligatoria</small>
            }
            @if (tieneError('entrada', 'min')) {
              <small class="error">Debe ser mayor que 0</small>
            }
            @if (errorDeGrupo('entradaExcede', 'entrada')) {
              <small class="error">
                Supera lo disponible del lote ({{ loteSeleccionado()?.cantidadDisponible | number: '1.0-2' }} {{ unidad() }})
              </small>
            }
          </div>

          <div class="campo">
            <label for="salida">Cantidad que sale</label>
            <p-inputnumber
              inputId="salida"
              formControlName="salida"
              [minFractionDigits]="0"
              [maxFractionDigits]="2"
              [suffix]="' ' + unidad()"
              locale="es-CO"
              placeholder="0"
              [fluid]="true"
            />
            @if (tieneError('salida', 'required')) {
              <small class="error">La cantidad de salida es obligatoria</small>
            }
            @if (tieneError('salida', 'min')) {
              <small class="error">Debe ser mayor que 0</small>
            }
            @if (errorDeGrupo('salidaExcede', 'salida')) {
              <small class="error">No puede ser mayor que la cantidad que entra</small>
            }
          </div>

          <div class="campo">
            <label for="codigoSalida">Código del lote resultante</label>
            <input pInputText id="codigoSalida" formControlName="codigoSalida" placeholder="Ej: L-001-SEC" />
            <small class="ayuda">Lo que sale del proceso queda como un lote nuevo con este código</small>
            @if (tieneError('codigoSalida', 'required')) {
              <small class="error">El código es obligatorio</small>
            }
            @if (tieneError('codigoSalida', 'maxlength')) {
              <small class="error">Máximo 30 caracteres</small>
            }
            @if (tieneError('codigoSalida', 'duplicado')) {
              <small class="error">Ya existe un lote con este código</small>
            }
          </div>

          <div class="campo">
            <label for="responsable">Responsable</label>
            <input pInputText id="responsable" [value]="auth.nombre()" disabled />
            <small class="ayuda">Se registra con el usuario que inició sesión</small>
          </div>

          <div class="campo ancho-completo">
            <label for="observaciones">Incidencias u observaciones <span class="opcional">(opcional)</span></label>
            <textarea
              pTextarea
              id="observaciones"
              formControlName="observaciones"
              rows="3"
              [autoResize]="true"
              placeholder="Ej: el secado se demoró por lluvia"
            ></textarea>
            @if (tieneError('observaciones', 'maxlength')) {
              <small class="error">Máximo 500 caracteres</small>
            }
          </div>

          @if (merma(); as m) {
            <div class="resumen ancho-completo">
              <i class="pi pi-chart-pie"></i>
              <span>
                Merma: <strong>{{ m.kg | number: '1.0-2' }} {{ unidad() }}</strong>
                ({{ m.porcentaje | number: '1.0-1' }} % de lo que entró)
              </span>
            </div>
          }

          <div class="acciones">
            <p-button label="Limpiar" severity="secondary" [text]="true" (onClick)="limpiar()" />
            <p-button type="submit" label="Registrar transformación" icon="pi pi-check" [loading]="guardando()" />
          </div>
        </form>
      </p-card>
    </section>
  `,
    styles: `
    /* Dos columnas en computador, una en celular (igual que Recepción) */
    .formulario {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1.25rem 1.5rem;
    }
    .campo {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    .ancho-completo {
      grid-column: 1 / -1;
    }
    label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--p-primary-900);
    }
    .opcional {
      font-weight: 400;
      color: var(--p-text-muted-color);
    }
    .ayuda {
      color: var(--p-text-muted-color);
    }
    .error {
      color: var(--p-red-500);
    }
    .resumen {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      padding: 0.75rem 1rem;
      border-radius: 1rem;
      background: var(--p-primary-50);
      color: var(--p-primary-800);
    }
    .mensaje-accion {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem 1rem;
    }
    .mensaje-accion a {
      color: inherit;
      font-weight: 600;
    }
    .acciones {
      grid-column: 1 / -1;
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }
    @media (max-width: 640px) {
      .formulario {
        grid-template-columns: 1fr;
      }
    }
  `
})
export class Transformaciones implements OnInit {
    protected readonly auth = inject(AuthService);
    private readonly lotesService = inject(LotesService);
    private readonly transformaciones = inject(TransformacionesService);
    private readonly mensajes = inject(MessageService);

    protected readonly etapas = ETAPAS;
    /** Tope del calendario: fin del día de hoy. Si fuera "ahora", la hora actual quedaría fuera del tope un minuto después. */
    protected readonly hoy = finDelDia();

    protected readonly lotes = signal<Lote[]>([]);
    protected readonly cargando = signal(true);
    protected readonly guardando = signal(false);
    protected readonly error = signal('');

    /** Solo se puede transformar un lote que todavía tenga cacao y que no esté bloqueado. */
    protected readonly opcionesLote = computed(() =>
        this.lotes()
            .filter((l) => l.cantidadDisponible > 0 && l.estado !== 'BLOQUEADO')
            .map((l) => ({
                id: l.id,
                etiqueta: `${l.codigo} · ${l.productor} (${l.cantidadDisponible} kg)`
            }))
    );

    /**
     * Validador de grupo: compara campos entre sí (HU08 — evitar consumos excesivos).
     * Va antes de `form` porque el formulario lo usa al crearse.
     */
    private readonly validarCantidades: ValidatorFn = (grupo) => {
        const loteId = grupo.get('loteId')?.value as number | null;
        const entrada = grupo.get('entrada')?.value as number | null;
        const salida = grupo.get('salida')?.value as number | null;
        const lote = this.lotes().find((l) => l.id === loteId);

        const errores: ValidationErrors = {};
        if (lote && entrada != null && entrada > lote.cantidadDisponible) errores['entradaExcede'] = true;
        if (entrada != null && salida != null && salida > entrada) errores['salidaExcede'] = true;
        return Object.keys(errores).length ? errores : null;
    };

    protected readonly form = inject(FormBuilder).group(
        {
            loteId: [null as number | null, Validators.required],
            tipo: [null as Etapa | null, Validators.required],
            fecha: [new Date() as Date | null, [Validators.required, fechaHoraNoFutura]],
            entrada: [null as number | null, [Validators.required, Validators.min(0.01)]],
            salida: [null as number | null, [Validators.required, Validators.min(0.01)]],
            codigoSalida: ['', [Validators.required, Validators.maxLength(30)]],
            observaciones: ['', Validators.maxLength(500)]
        },
        { validators: this.validarCantidades }
    );

    constructor() {
        // Al elegir lote o etapa, se propone un código para el lote resultante.
        merge(this.form.controls.loteId.valueChanges, this.form.controls.tipo.valueChanges)
            .pipe(takeUntilDestroyed())
            .subscribe(() => this.sugerirCodigoSalida());
    }

    ngOnInit(): void {
        this.cargarLotes();
    }

    protected cargarLotes(): void {
        this.cargando.set(true);
        this.error.set('');
        this.lotesService.listar().subscribe({
            next: (lotes) => {
                this.lotes.set(lotes);
                this.cargando.set(false);
                this.form.updateValueAndValidity(); // el disponible cambió: revalidar cantidades
            },
            error: () => {
                this.error.set('No se pudieron cargar los lotes');
                this.cargando.set(false);
            }
        });
    }

    protected loteSeleccionado(): Lote | undefined {
        const id = this.form.controls.loteId.value;
        return this.lotes().find((l) => l.id === id);
    }

    protected unidad(): string {
        return (this.loteSeleccionado()?.unidad ?? 'KG').toLowerCase(); // el backend manda "KG"
    }

    /** Diferencia entre lo que entra y lo que sale. null si aún no se puede calcular. */
    protected merma(): { kg: number; porcentaje: number } | null {
        const { entrada, salida } = this.form.getRawValue();
        if (!entrada || salida == null || salida > entrada) return null;
        const kg = Math.round((entrada - salida) * 100) / 100;
        return { kg, porcentaje: (kg / entrada) * 100 };
    }

    /** true si el campo ya fue tocado y tiene ese error. */
    protected tieneError(campo: string, error: string): boolean {
        const control = this.form.get(campo);
        return !!control && control.touched && control.hasError(error);
    }

    /** Igual que tieneError, pero para errores del validador de grupo. */
    protected errorDeGrupo(error: string, campo: string): boolean {
        return this.form.hasError(error) && !!this.form.get(campo)?.touched;
    }

    protected guardar(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const valores = this.form.getRawValue();
        const datos: TransformacionRequest = {
            tipo: valores.tipo!,
            fecha: aFechaHoraIso(valores.fecha!),
            entradas: [{ loteId: valores.loteId!, cantidadKg: valores.entrada! }],
            salidas: [{ codigo: valores.codigoSalida!.trim(), cantidadKg: valores.salida! }],
            observaciones: valores.observaciones?.trim() || undefined
        };
        const merma = this.merma();
        const unidad = this.unidad();

        this.guardando.set(true);
        this.transformaciones.registrar(datos).subscribe({
            next: (t) => {
                this.guardando.set(false);
                this.mensajes.add({
                    severity: 'success',
                    summary: `Transformación ${t.codigo} registrada`,
                    detail: merma ? `Merma: ${merma.kg} ${unidad} (${merma.porcentaje.toFixed(1)} %)` : undefined
                });
                this.limpiar();
                this.cargarLotes();
            },
            error: (err: HttpErrorResponse) => {
                this.guardando.set(false);
                if (esCodigoDuplicado(err)) {
                    this.form.controls.codigoSalida.setErrors({ duplicado: true });
                    return;
                }
                this.mensajes.add({ severity: 'error', summary: 'No se pudo registrar', detail: mensajeDeError(err) });
                // Si el backend rechazó por cantidades, el disponible pudo cambiar: recargar los lotes
                this.cargarLotes();
            }
        });
    }

    protected limpiar(): void {
        this.form.reset({ fecha: new Date(), codigoSalida: '', observaciones: '' });
    }

    /** Propone "<lote>-<etapa>" (ej. L-001-SEC) mientras el operador no lo haya escrito a mano. */
    private sugerirCodigoSalida(): void {
        const control = this.form.controls.codigoSalida;
        if (control.dirty) return;
        const lote = this.loteSeleccionado();
        const tipo = this.form.controls.tipo.value;
        if (lote && tipo) control.setValue(`${lote.codigo}-${tipo.slice(0, 3)}`);
    }
}
