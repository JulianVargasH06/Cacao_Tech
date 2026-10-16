import { Routes } from '@angular/router';

/** Páginas del administrador (Frontend 2). El rolGuard('ADMINISTRADOR') está en app.routes.ts. */
export const ADMIN_ROUTES: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'panel' },
    {
        path: 'panel',
        loadComponent: () => import('./panel/panel').then((m) => m.Panel)
    },
    {
        path: 'productos',
        loadComponent: () => import('./productos/productos').then((m) => m.Productos)
    },
    {
        path: 'mermas-costos',
        loadComponent: () => import('./mermas-costos/mermas-costos').then((m) => m.MermasCostos)
    },
    {
        path: 'trazabilidad',
        loadComponent: () => import('./trazabilidad/trazabilidad').then((m) => m.Trazabilidad)
    }
];
