import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, delay, map, of, tap } from 'rxjs';

import { API_URL } from '../config/api.config';
import { USAR_MOCK_AUTH } from '../config/dev.config';
import { LoginRequest, LoginResponse, Rol, Sesion, normalizarRol } from '../models/auth.model';

const CLAVE_SESION = 'cacao_sesion';

@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

    private readonly _sesion = signal<Sesion | null>(this.leerSesion());

    readonly sesion = this._sesion.asReadonly();
    readonly estaAutenticado = computed(() => !!this._sesion()?.token);
    readonly rol = computed<Rol | null>(() => this._sesion()?.rol ?? null);
    readonly nombre = computed(() => this._sesion()?.nombre ?? '');
    readonly correo = computed(() => this._sesion()?.correo ?? '');

    token(): string | null {
        return this._sesion()?.token ?? null;
    }

    /** POST /api/auth/login (o simulado si USAR_MOCK_AUTH = true). */
    login(datos: LoginRequest): Observable<Sesion> {
        const respuesta$: Observable<LoginResponse> = USAR_MOCK_AUTH
            ? of({
                token: 'token-de-prueba',
                correo: datos.correo,
                rol: datos.correo.toLowerCase().includes('admin') ? 'ADMINISTRADOR' : 'OPERADOR'
            }).pipe(delay(400))
            : this.http.post<LoginResponse>(`${API_URL}/auth/login`, datos);

        return respuesta$.pipe(
            map((r) => ({ token: r.token, rol: normalizarRol(r.rol), nombre: r.nombre ?? r.correo, correo: r.correo })),
            tap((sesion) => this.guardarSesion(sesion))
        );
    }

    logout(): void {
        this._sesion.set(null);
        if (this.esNavegador) {
            try {
                localStorage.removeItem(CLAVE_SESION);
            } catch {
                /* sin almacenamiento disponible */
            }
        }
        this.router.navigate(['/login']);
    }

    /** Página a la que se redirige según el rol (HU13). */
    rutaInicial(rol: Rol | null = this.rol()): string {
        return rol === 'ADMINISTRADOR' ? '/admin/panel' : '/operador/recepcion';
    }

    private guardarSesion(sesion: Sesion): void {
        this._sesion.set(sesion);
        if (this.esNavegador) {
            try {
                localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
            } catch {
                /* sin almacenamiento disponible */
            }
        }
    }

    private leerSesion(): Sesion | null {
        if (!this.esNavegador) return null;
        try {
            const crudo = localStorage.getItem(CLAVE_SESION);
            return crudo ? (JSON.parse(crudo) as Sesion) : null;
        } catch {
            return null;
        }
    }
}