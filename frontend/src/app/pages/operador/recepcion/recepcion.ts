import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { LoteRequest, UNIDADES, Unidad } from '../../../core/models/lote.model';
import { LotesService } from '../../../core/services/lotes.service';
import { PageHeader } from '../../../shared/page-header/page-header';

/** Valida que la fecha no sea posterior a hoy. */
function fechaNoFutura(control: AbstractControl): ValidationErrors | null {
    const fecha = control.value as Date | null;
    if (!fecha) return null; // si está vacía, de eso se encarga Validators.required
    const finDeHoy = new Date();
    finDeHoy.setHours(23, 59, 59, 999);
    return fecha > finDeHoy ? { fechaFutura: true } : null;
}

function aFechaIso(fecha: Date): string {
    const anio = fecha.getFullYear();
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
}

@Component({
    selector: 'app-recepcion',
    imports: [
        ReactiveFormsModule,
        PageHeader,
        CardModule,
        InputTextModule,
        InputNumberModule,
        DatePickerModule,
        SelectModule,
        ButtonModule,
        ToastModule
    ],
    providers: [MessageService],
    template: `
    <section class="page">
        <p-toast />
      <app-page-header
        titulo="Recepción de lotes"
        descripcion="Registra los lotes de cacao que llegan a la asociación"
        historia="HU01"
        rol="Operador"
      />

      <p-card>
        <form [formGroup]="form" (ngSubmit)="guardar()" class="formulario">
          <div class="campo">
            <label for="codigo">Código del lote</label>
            <input pInputText id="codigo" formControlName="codigo" placeholder="Ej: L-003" />
            @if (tieneError('codigo', 'required')) {
              <small class="error">El código es obligatorio</small>
            }
            @if (tieneError('codigo', 'maxlength')) {
              <small class="error">Máximo 20 caracteres</small>
            }
            @if (tieneError('codigo', 'duplicado')) {
                <small class="error">Ya existe un lote con este código</small>
                }
          </div>

          <div class="campo">
            <label for="fecha">Fecha de recepción</label>
            <p-datepicker
              inputId="fecha"
              formControlName="fechaRecepcion"
              [maxDate]="hoy"
              dateFormat="dd/mm/yy"
              [showIcon]="true"
              [fluid]="true"
            />
            @if (tieneError('fechaRecepcion', 'required')) {
              <small class="error">La fecha es obligatoria</small>
            }
            @if (tieneError('fechaRecepcion', 'fechaFutura')) {
              <small class="error">La fecha no puede ser futura</small>
            }
          </div>

          <div class="campo">
            <label for="productor">Productor</label>
            <input pInputText id="productor" formControlName="productor" placeholder="Nombre del productor o finca" />
            @if (tieneError('productor', 'required')) {
              <small class="error">El productor es obligatorio</small>
            }
          </div>

          <div class="campo">
            <label for="origen">Origen</label>
            <input pInputText id="origen" formControlName="origen" placeholder="Ej: Rivera, Huila" />
            @if (tieneError('origen', 'required')) {
              <small class="error">El origen es obligatorio</small>
            }
          </div>

          <div class="campo">
            <label for="cantidad">Cantidad recibida</label>
            <p-inputnumber
              inputId="cantidad"
              formControlName="cantidadRecibida"
              [minFractionDigits]="0"
              [maxFractionDigits]="2"
              locale="es-CO"
              placeholder="0"
              [fluid]="true"
            />
            @if (tieneError('cantidadRecibida', 'required')) {
              <small class="error">La cantidad es obligatoria</small>
            }
            @if (tieneError('cantidadRecibida', 'min')) {
              <small class="error">La cantidad debe ser mayor que 0</small>
            }
          </div>

          <div class="campo">
            <label for="unidad">Unidad</label>
            <p-select inputId="unidad" formControlName="unidad" [options]="unidades" [fluid]="true" />
          </div>

          <div class="acciones">
            <p-button label="Limpiar" severity="secondary" [text]="true" (onClick)="limpiar()" />
            <p-button type="submit" label="Registrar lote" icon="pi pi-check" [loading]="guardando()" />
          </div>
        </form>
      </p-card>
    </section>
  `,
    styles: `
    /* Dos columnas en computador, una en celular */
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
    label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--p-primary-900);
    }
    .error {
      color: var(--p-red-500);
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
export class Recepcion {
    private readonly lotes = inject(LotesService);
    private readonly mensajes = inject(MessageService);
    protected readonly guardando = signal(false);
    protected readonly unidades = UNIDADES;
    protected readonly hoy = new Date();

    protected readonly form = inject(FormBuilder).group({
        codigo: ['', [Validators.required, Validators.maxLength(20)]],
        fechaRecepcion: [new Date() as Date | null, [Validators.required, fechaNoFutura]],
        productor: ['', Validators.required],
        origen: ['', Validators.required],
        cantidadRecibida: [null as number | null, [Validators.required, Validators.min(0.01)]],
        unidad: ['kg' as Unidad, Validators.required]
    });

    /** true si el campo ya fue tocado y tiene ese error. */
    protected tieneError(campo: string, error: string): boolean {
        const control = this.form.get(campo);
        return !!control && control.touched && control.hasError(error);
    }

    protected guardar(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const valores = this.form.getRawValue();
        const datos: LoteRequest = {
            codigo: valores.codigo!.trim(),
            productor: valores.productor!.trim(),
            origen: valores.origen!.trim(),
            fechaRecepcion: aFechaIso(valores.fechaRecepcion!),
            cantidadRecibida: valores.cantidadRecibida!,
            unidad: valores.unidad!
        };

        this.guardando.set(true);
        this.lotes.registrar(datos).subscribe({
            next: (lote) => {
                this.guardando.set(false);
                this.mensajes.add({
                    severity: 'success',
                    summary: 'Lote registrado',
                    detail: `${lote.codigo} · ${lote.cantidadRecibida} ${lote.unidad}`
                });
                this.limpiar();
            },
            error: (err: HttpErrorResponse) => {
                this.guardando.set(false);
                if (err.status === 409) {
                    this.form.controls.codigo.setErrors({ duplicado: true });
                    return;
                }
                this.mensajes.add({
                    severity: 'error',
                    summary: 'No se pudo registrar',
                    detail: err.error?.mensaje ?? 'Intenta de nuevo en unos segundos'
                });
            }
        });
    }

    protected limpiar(): void {
        this.form.reset({ fechaRecepcion: new Date(), unidad: 'kg' });
    }
}