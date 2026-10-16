import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { MenuModule } from 'primeng/menu';

import { AuthService } from '../../core/services/auth.service';

/**
 * Estructura para las páginas internas (con login).
 *  - Computador: menú lateral fijo + barra superior.
 *  - Celular/tablet (≤ 900px): el menú lateral se oculta y se abre con el botón de hamburguesa.
 */
@Component({
    selector: 'app-main-layout',
    imports: [RouterOutlet, MenuModule, ButtonModule, DrawerModule],
    template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand"><i class="pi pi-box"></i> CacaoTech</div>
        <p-menu [model]="menu()" />
      </aside>

      <div class="main">
        <header class="topbar">
          <p-button
            class="boton-menu"
            icon="pi pi-bars"
            [rounded]="true"
            [text]="true"
            aria-label="Abrir menú"
            (onClick)="menuAbierto.set(true)"
          />
          <span class="brand-movil"><i class="pi pi-box"></i> CacaoTech</span>

          <span class="espacio"></span>

          <span class="usuario">
            <i class="pi pi-user"></i>
            <span class="usuario-texto">{{ auth.nombre() }} · {{ auth.rol() }}</span>
          </span>
          <p-button label="Salir" icon="pi pi-sign-out" severity="secondary" [text]="true" (onClick)="auth.logout()" />
        </header>

        <main class="contenido">
          <router-outlet />
        </main>
      </div>
    </div>

    <!-- Menú deslizable para pantallas pequeñas -->
    <p-drawer [(visible)]="menuAbierto" position="left" [style]="{ width: '18rem' }">
      <ng-template #header>
        <div class="brand"><i class="pi pi-box"></i> CacaoTech</div>
      </ng-template>
      <p-menu [model]="menu()" />
    </p-drawer>
  `,
    styles: `
    .shell {
      display: grid;
      grid-template-columns: 17rem 1fr;
      gap: 1rem;
      padding: 1rem;
      min-height: 100vh;
    }

    /* Paneles de cristal flotantes */
    .sidebar,
    .topbar {
      background: rgba(255, 255, 255, 0.55);
      backdrop-filter: blur(18px) saturate(140%);
      -webkit-backdrop-filter: blur(18px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.7);
      box-shadow: 0 10px 30px rgba(117, 74, 49, 0.1);
    }

    .sidebar {
      position: sticky;
      top: 1rem;
      height: calc(100vh - 2rem);
      border-radius: 1.75rem;
      padding: 1.25rem 1rem;
    }
    .brand {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--p-primary-700);
      margin: 0.25rem 0.5rem 1.25rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .main {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      min-width: 0;
    }
    .topbar {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.4rem 0.6rem 0.4rem 1.4rem;
      border-radius: 999px;
    }
    .espacio {
      flex: 1;
    }
    .usuario {
      color: var(--p-text-muted-color);
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .contenido {
      padding: 0.5rem 0.5rem 1.5rem;
    }

    /* El botón de hamburguesa y el logo móvil solo aparecen en pantallas pequeñas */
    .boton-menu,
    .brand-movil {
      display: none;
    }
    .brand-movil {
      font-weight: 700;
      color: var(--p-primary-700);
      align-items: center;
      gap: 0.4rem;
    }

    /* Tablet y celular */
    @media (max-width: 900px) {
      .shell {
        grid-template-columns: 1fr;
        padding: 0.75rem;
        gap: 0.75rem;
      }
      .sidebar {
        display: none;
      }
      .main {
        gap: 0.75rem;
      }
      .topbar {
        padding: 0.35rem 0.5rem;
      }
      .boton-menu {
        display: inline-flex;
      }
      .brand-movil {
        display: flex;
      }
    }

    /* Celular pequeño: solo el ícono de usuario, sin el texto */
    @media (max-width: 520px) {
      .usuario-texto {
        display: none;
      }
    }
  `
})
export class MainLayout {
    protected readonly auth = inject(AuthService);

    protected readonly menuAbierto = signal(false);

    constructor() {
        // Al navegar a otra página, se cierra el menú deslizable.
        inject(Router)
            .events.pipe(
                filter((e) => e instanceof NavigationEnd),
                takeUntilDestroyed()
            )
            .subscribe(() => this.menuAbierto.set(false));
    }

    /** Opciones del menú según el rol del usuario. */
    protected readonly menu = computed<MenuItem[]>(() => {
        const historial: MenuItem = { label: 'Historial', icon: 'pi pi-history', routerLink: '/historial' };

        if (this.auth.rol() === 'ADMINISTRADOR') {
            return [
                { label: 'Panel', icon: 'pi pi-chart-bar', routerLink: '/admin/panel' },
                { label: 'Mermas y costos', icon: 'pi pi-percentage', routerLink: '/admin/mermas-costos' },
                { label: 'Trazabilidad', icon: 'pi pi-sitemap', routerLink: '/admin/trazabilidad' },
                { label: 'Productos', icon: 'pi pi-tag', routerLink: '/admin/productos' },
                { label: 'Usuarios', icon: 'pi pi-users', routerLink: '/admin/usuarios' },
                { label: 'Auditoría', icon: 'pi pi-shield', routerLink: '/admin/auditoria' },
                historial
            ];
        }

        return [
            { label: 'Recepción de lotes', icon: 'pi pi-inbox', routerLink: '/operador/recepcion' },
            { label: 'Transformaciones', icon: 'pi pi-sync', routerLink: '/operador/transformaciones' },
            { label: 'Inventario', icon: 'pi pi-database', routerLink: '/operador/inventario' },
            historial
        ];
    });
}