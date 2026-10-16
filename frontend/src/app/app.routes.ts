import { Routes } from '@angular/router';

import { authGuard, rolGuard } from './core/guards/auth.guard';
import { MainLayout } from './layout/main-layout/main-layout';
import { PublicLayout } from './layout/public-layout/public-layout';

export const routes: Routes = [
    {
        // Página de inicio informativa (estática): lleva al login y a la consulta pública del producto
        path: '',
        pathMatch: 'full',
        loadComponent: () => import('./pages/landing/landing').then((m) => m.Landing)
    },
    {
        // Páginas públicas (sin login): la ficha comercial del producto (HU06)
        path: 'publico',
        component: PublicLayout,
        children: [
            { path: '', pathMatch: 'full', redirectTo: 'producto' },
            { path: 'producto', loadComponent: () => import('./pages/publico/ficha/ficha').then((m) => m.Ficha) },
            { path: 'producto/:codigo', loadComponent: () => import('./pages/publico/ficha/ficha').then((m) => m.Ficha) }
        ]
    },
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