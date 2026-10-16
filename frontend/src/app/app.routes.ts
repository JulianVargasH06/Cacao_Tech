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
            },
            {
                // Compartida: la ven ambos roles (solo pide sesión, por el authGuard del layout)
                path: 'historial',
                loadComponent: () => import('./pages/historial/historial').then((m) => m.Historial)
            },
            {
                path: 'admin',
                canActivate: [rolGuard('ADMINISTRADOR')],
                loadChildren: () => import('./pages/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
            }
        ]
    },
    { path: '**', redirectTo: 'login' }
];