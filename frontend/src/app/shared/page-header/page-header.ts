import { Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';

/** Encabezado estándar de cada página: título, descripción y la HU que cubre. Ejemplo:
 *  <app-page-header titulo="Inventario" historia="HU08" rol="Operador" />
 */
@Component({
    selector: 'app-page-header',
    imports: [TagModule],
    template: `
    <header class="header">
      <div>
        <h1>{{ titulo() }}</h1>
        @if (descripcion()) {
          <p>{{ descripcion() }}</p>
        }
      </div>
      <div class="tags">
        @if (historia()) {
          <p-tag [value]="historia()" severity="secondary" />
        }
        @if (rol()) {
          <p-tag [value]="rol()" />
        }
      </div>
    </header>
  `,
    styles: `
    .header {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: flex-start;
      gap: 0.75rem 1rem;
      margin-bottom: 1rem;
    }
    h1 {
      margin: 0;
      font-size: 1.5rem;
    }
    p {
      margin: 0.25rem 0 0;
      color: var(--p-text-muted-color);
    }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    @media (max-width: 520px) {
      h1 {
        font-size: 1.25rem;
      }
    }
  `
})
export class PageHeader {
    readonly titulo = input.required<string>();
    readonly descripcion = input('');
    readonly historia = input('');
    readonly rol = input('');
}