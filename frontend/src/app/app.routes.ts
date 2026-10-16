import { Routes } from '@angular/router';

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
        children: [
            {
                path: 'operador',
                loadChildren: () => import('./pages/operador/operador.routes').then((m) => m.OPERADOR_ROUTES)
            },
            // Frontend 2: { path: 'admin', loadChildren: () => import('./pages/admin/admin.routes')... }
        ]
    }
];