import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Estructura para páginas públicas (sin login), como la ficha comercial. */
@Component({
    selector: 'app-public-layout',
    imports: [RouterOutlet],
    template: `
    <header class="header"><i class="pi pi-box"></i> CacaoTech</header>
    <main class="contenido">
      <router-outlet />
    </main>
  `,
    styles: `
    .header {
      background: var(--p-primary-700);
      color: #fff;
      padding: 0.85rem 1.5rem;
      font-weight: 700;
      font-size: 1.1rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .contenido {
      max-width: 60rem;
      margin: 0 auto;
      padding: 1.5rem;
    }
  `
})
export class PublicLayout { }