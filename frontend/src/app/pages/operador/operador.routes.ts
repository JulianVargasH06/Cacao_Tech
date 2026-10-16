import { Routes } from '@angular/router';

export const OPERADOR_ROUTES: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'recepcion' },
    {
        path: 'recepcion',
        loadComponent: () => import('./recepcion/recepcion').then((m) => m.Recepcion)
    }
];