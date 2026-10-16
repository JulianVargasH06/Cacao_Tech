import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { API_URL } from '../config/api.config';
import { AuthService } from '../services/auth.service';

/**
 * - Agrega "Authorization: Bearer <token>" a todas las llamadas a /api (menos login y /publico).
 * - Si el backend responde 401, cierra la sesión y manda al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const auth = inject(AuthService);

    const esApi = req.url.startsWith(API_URL);
    const esLibre = req.url.startsWith(`${API_URL}/publico`) || req.url.startsWith(`${API_URL}/auth/login`);
    const token = auth.token();

    const peticion =
        esApi && !esLibre && token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

    return next(peticion).pipe(
        catchError((error: HttpErrorResponse) => {
            if (error.status === 401 && esApi && !esLibre) {
                auth.logout();
            }
            return throwError(() => error);
        })
    );
};