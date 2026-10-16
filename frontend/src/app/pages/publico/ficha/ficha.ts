import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';

import { FichaPublica } from '../../../core/models/producto.model';
import { PublicoService } from '../../../core/services/publico.service';
import { mensajeDeError } from '../../../shared/utils/errores-api';

/**
 * Ficha comercial pública (HU06): el comprador consulta un producto por su código, sin iniciar sesión.
 * La dirección /publico/producto/CODIGO sirve para imprimirla como código QR en el empaque.
 */
@Component({
    selector: 'app-ficha',
    imports: [FormsModule, DatePipe, CardModule, InputTextModule, ButtonModule, MessageModule, TagModule, TimelineModule],
    template: `
    <section class="page">
      <p-card>
        <form class="buscador" (ngSubmit)="buscar()">
          <div>
            <h1>Conoce el origen de tu cacao</h1>
            <p>Escribe el código que aparece en el empaque</p>
          </div>
          <div class="campo-busqueda">
            <input pInputText name="codigo" [(ngModel)]="busqueda" placeholder="Ej: CAC-2026-020-TOS" aria-label="Código del producto" />
            <p-button type="submit" label="Consultar" icon="pi pi-search" [loading]="cargando()" [disabled]="!busqueda.trim()" />
          </div>
        </form>
      </p-card>

      @if (error()) {
        <p-message [severity]="noEncontrado() ? 'warn' : 'error'" [text]="error()" />
      }

      @if (ficha(); as f) {
        <p-card>
          <div class="cabecera">
            <div>
              <span class="tipo">{{ f.tipo }}</span>
              <h2>{{ f.codigo }}</h2>
            </div>
            <p-tag [value]="f.estadoDescripcion" [severity]="severidad(f.estado)" [rounded]="true" />
          </div>

          <div class="datos">
            <div>
              <span class="etiqueta"><i class="pi pi-home"></i> Productor</span>
              <strong>{{ f.productor || '—' }}</strong>
            </div>
            <div>
              <span class="etiqueta"><i class="pi pi-map-marker"></i> Origen</span>
              <strong>{{ f.origen || '—' }}</strong>
            </div>
            <div>
              <span class="etiqueta"><i class="pi pi-calendar"></i> Registro</span>
              <strong>{{ f.fechaRegistro | date: 'dd/MM/yyyy' }}</strong>
            </div>
          </div>
        </p-card>

        <p-card header="Recorrido del cacao">
          <p-timeline [value]="f.recorrido" class="recorrido">
            <ng-template #marker let-paso>
              <span class="marcador" [class.marcador-inicio]="paso.etapa === 'RECEPCION'">
                <i [class]="icono(paso.etapa)"></i>
              </span>
            </ng-template>
            <ng-template #content let-paso>
              <div class="paso">
                <strong>{{ nombreEtapa(paso.etapa) }}</strong>
                <small>{{ paso.fecha | date: (paso.etapa === 'RECEPCION' ? 'dd/MM/yyyy' : 'dd/MM/yyyy HH:mm') }}</small>
                <p>{{ paso.descripcion }}</p>
              </div>
            </ng-template>
          </p-timeline>
        </p-card>
      }
    </section>
  `,
    styles: `
    .buscador {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
    }
    h1 {
      margin: 0;
      font-size: 1.35rem;
      color: var(--p-primary-800);
    }
    .buscador p {
      margin: 0.25rem 0 0;
      color: var(--p-text-muted-color);
    }
    .campo-busqueda {
      display: flex;
      gap: 0.5rem;
      flex: 1 1 20rem;
      max-width: 26rem;
    }
    .campo-busqueda input {
      flex: 1;
      min-width: 0;
    }
    .cabecera {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .tipo {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--p-text-muted-color);
    }
    h2 {
      margin: 0.2rem 0 0;
      font-size: 1.6rem;
      color: var(--p-primary-900);
    }
    .datos {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
      gap: 1rem;
      margin-top: 1.25rem;
    }
    .datos div {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .etiqueta {
      font-size: 0.85rem;
      color: var(--p-text-muted-color);
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }
    .marcador {
      width: 2.25rem;
      height: 2.25rem;
      border-radius: 50%;
      display: grid;
      place-items: center;
      background: var(--p-primary-600);
      color: #fff;
    }
    .marcador-inicio {
      background: var(--p-green-600);
    }
    .paso {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
      padding-bottom: 1rem;
    }
    .paso small {
      color: var(--p-text-muted-color);
    }
    .paso p {
      margin: 0.25rem 0 0;
    }
    /* Línea de tiempo alineada a la izquierda: sin la columna vacía del lado opuesto */
    :host ::ng-deep .recorrido .p-timeline-event-opposite {
      display: none;
    }
  `
})
export class Ficha {
    private readonly publico = inject(PublicoService);
    private readonly router = inject(Router);

    protected busqueda = '';
    protected readonly ficha = signal<FichaPublica | null>(null);
    protected readonly cargando = signal(false);
    protected readonly error = signal('');
    protected readonly noEncontrado = signal(false);

    constructor() {
        // El código viene en la URL: así el mismo enlace (o QR) siempre muestra el mismo producto
        inject(ActivatedRoute)
            .paramMap.pipe(takeUntilDestroyed())
            .subscribe((params) => {
                const codigo = params.get('codigo');
                if (codigo) {
                    this.busqueda = codigo;
                    this.consultar(codigo);
                }
            });
    }

    protected buscar(): void {
        const codigo = this.busqueda.trim();
        if (codigo) this.router.navigate(['/publico/producto', codigo.toUpperCase()]);
    }

    protected severidad(estado: string): 'success' | 'warn' | 'info' {
        if (estado === 'LIBERADO') return 'success';
        if (estado === 'BLOQUEADO') return 'warn';
        return 'info';
    }

    protected icono(etapa: string): string {
        const iconos: Record<string, string> = {
            RECEPCION: 'pi pi-home',
            FERMENTACION: 'pi pi-clock',
            SECADO: 'pi pi-sun',
            TOSTADO: 'pi pi-bolt',
            MOLIENDA: 'pi pi-cog',
            EMPAQUE: 'pi pi-box'
        };
        return iconos[etapa] ?? 'pi pi-circle';
    }

    /** "FERMENTACION" → "Fermentación" */
    protected nombreEtapa(etapa: string): string {
        const nombres: Record<string, string> = {
            RECEPCION: 'Recepción en la asociación',
            FERMENTACION: 'Fermentación',
            SECADO: 'Secado',
            TOSTADO: 'Tostado',
            MOLIENDA: 'Molienda',
            EMPAQUE: 'Empaque'
        };
        return nombres[etapa] ?? etapa.charAt(0) + etapa.slice(1).toLowerCase();
    }

    private consultar(codigo: string): void {
        this.cargando.set(true);
        this.error.set('');
        this.noEncontrado.set(false);
        this.ficha.set(null);
        this.publico.obtenerFicha(codigo).subscribe({
            next: (ficha) => {
                this.ficha.set(ficha);
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.noEncontrado.set(err.status === 404);
                this.error.set(mensajeDeError(err, 'No se pudo consultar el producto'));
                this.cargando.set(false);
            }
        });
    }
}
