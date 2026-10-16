import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Rol } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

/** Solo deja pasar a usuarios con sesión iniciada. */
export const authGuard: CanActivateFn = () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.estaAutenticado() ? true : router.createUrlTree(['/login']);
};

/** Solo deja pasar a los roles indicados. Uso: canActivate: [rolGuard('ADMINISTRADOR')] */
export const rolGuard =
    (...roles: Rol[]): CanActivateFn =>
        () => {
            const auth = inject(AuthService);
            const router = inject(Router);

            if (!auth.estaAutenticado()) return router.createUrlTree(['/login']);

            const rol = auth.rol();
            if (rol && roles.includes(rol)) return true;

            // Tiene sesión pero no permiso: lo mandamos a su página de inicio.
            return router.createUrlTree([auth.rutaInicial()]);
        };