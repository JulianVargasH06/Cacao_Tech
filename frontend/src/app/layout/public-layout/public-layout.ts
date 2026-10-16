import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Estructura para páginas públicas (sin login), como la ficha comercial del producto (HU06). */
@Component({
    selector: 'app-public-layout',
    imports: [RouterOutlet],
    template: `
    <div class="envoltura">
      <header class="header">
        <span class="brand"><i class="pi pi-box"></i> CacaoTech</span>
        <span class="lema">Trazabilidad del cacao, de la finca a tus manos</span>
      </header>
      <main class="contenido">
        <router-outlet />
      </main>
    </div>
  `,
    styles: `
    .envoltura {
      max-width: 60rem;
      margin: 0 auto;
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }
    /* Misma barra de cristal en píldora que el layout interno */
    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 0.5rem 1rem;
      padding: 0.75rem 1.4rem;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.55);
      backdrop-filter: blur(18px) saturate(140%);
      -webkit-backdrop-filter: blur(18px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.7);
      box-shadow: 0 10px 30px rgba(117, 74, 49, 0.1);
    }
    .brand {
      font-weight: 700;
      font-size: 1.15rem;
      color: var(--p-primary-700);
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .lema {
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
    }
    .contenido {
      padding: 0 0.25rem 1.5rem;
    }
    @media (max-width: 520px) {
      .header {
        border-radius: 1.5rem;
      }
    }
  `
})
export class PublicLayout { }
