import { Routes } from '@angular/router';

export const OPERADOR_ROUTES: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'recepcion' },
    {
        path: 'recepcion',
        loadComponent: () => import('./recepcion/recepcion').then((m) => m.Recepcion)
    },
    {
        path: 'inventario',
        loadComponent: () => import('./inventario/inventario').then((m) => m.Inventario)
    },
    {
        path: 'transformaciones',
        loadComponent: () => import('./transformaciones/transformaciones').then((m) => m.Transformaciones)
    }
];