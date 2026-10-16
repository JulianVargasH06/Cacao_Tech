import { Routes } from '@angular/router';

import { authGuard, rolGuard } from './core/guards/auth.guard';
import { MainLayout } from './layout/main-layout/main-layout';

export const routes: Routes = [
    { path: '', pathMatch: 'full', redirectTo: 'operador' },
    {
        path: 'login',
        loadComponent: () => import('./pages/login/login').then((m) => m.Login)
    },
    {
        path: '',
        component: MainLayout,
        canActivate: [authGuard],
        children: [
            {
                path: 'operador',
                canActivate: [rolGuard('OPERADOR')],
                loadChildren: () => import('./pages/operador/operador.routes').then((m) => m.OPERADOR_ROUTES)
            }
            // Frontend 2: { path: 'admin', canActivate: [rolGuard('ADMINISTRADOR')], loadChildren: ... }
        ]
    },
    { path: '**', redirectTo: 'login' }
];