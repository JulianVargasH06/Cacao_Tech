import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';

import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, RouterLink, ButtonModule, InputTextModule, PasswordModule, MessageModule],
    template: `
    <main class="pantalla">
      <section class="tarjeta">
        <div class="logo"><i class="pi pi-box"></i></div>
        <h1>CacaoTech</h1>
        <p class="subtitulo">Trazabilidad del cacao, del lote al producto</p>

        <form [formGroup]="form" (ngSubmit)="entrar()">
          <label for="correo">Correo</label>
          <input
            pInputText
            id="correo"
            type="email"
            formControlName="correo"
            autocomplete="username"
            placeholder="tu.correo@cacaotech.com"
          />
          @if (form.controls.correo.touched && form.controls.correo.hasError('email')) {
            <small class="error-campo">Escribe un correo válido</small>
          }

          <label for="password">Contraseña</label>
          <p-password
            inputId="password"
            formControlName="password"
            [feedback]="false"
            [toggleMask]="true"
            [fluid]="true"
            autocomplete="current-password"
            placeholder="Tu contraseña"
          />

          @if (error()) {
            <p-message severity="error" [text]="error()" />
          }

          <p-button
            type="submit"
            label="Ingresar"
            icon="pi pi-arrow-right"
            iconPos="right"
            [loading]="cargando()"
            [disabled]="form.invalid"
            [fluid]="true"
          />
        </form>

        <a routerLink="/publico/producto" class="enlace-comprador">
          <i class="pi pi-search"></i> ¿Compraste un producto? Consulta su origen
        </a>
      </section>
    </main>
  `,
    styles: `
    .pantalla {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 1rem;
    }
    /* Mismo cristal que la barra y el menú del MainLayout */
    .tarjeta {
      width: min(100%, 24rem);
      padding: 2.25rem 2rem;
      border-radius: 1.75rem;
      background: rgba(255, 255, 255, 0.6);
      backdrop-filter: blur(18px) saturate(140%);
      -webkit-backdrop-filter: blur(18px) saturate(140%);
      border: 1px solid rgba(255, 255, 255, 0.7);
      box-shadow: 0 20px 50px rgba(117, 74, 49, 0.16);
      text-align: center;
    }
    .logo {
      width: 3.5rem;
      height: 3.5rem;
      margin: 0 auto 0.75rem;
      border-radius: 1.1rem;
      display: grid;
      place-items: center;
      background: linear-gradient(135deg, var(--p-primary-500), var(--p-primary-800));
      color: #fff;
      font-size: 1.5rem;
    }
    h1 {
      margin: 0;
      font-size: 1.6rem;
      color: var(--p-primary-800);
    }
    .subtitulo {
      margin: 0.3rem 0 1.75rem;
      color: var(--p-text-muted-color);
    }
    form {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      text-align: left;
    }
    label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--p-primary-900);
      margin-top: 0.4rem;
    }
    input {
      width: 100%;
    }
    p-button {
      margin-top: 1rem;
    }
    .enlace-comprador {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      margin-top: 1.5rem;
      font-size: 0.875rem;
      color: var(--p-primary-700);
      text-decoration: none;
    }
    .enlace-comprador:hover {
      text-decoration: underline;
    }
    .error-campo {
      color: var(--p-red-500);
    }
  `
})
export class Login {
    private readonly auth = inject(AuthService);
    private readonly router = inject(Router);

    protected readonly cargando = signal(false);
    protected readonly error = signal('');

    protected readonly form = inject(FormBuilder).nonNullable.group({
        correo: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required]
    });

    protected entrar(): void {
        if (this.form.invalid) return;
        this.cargando.set(true);
        this.error.set('');

        const { correo, password } = this.form.getRawValue();
        this.auth.login({ correo: correo.trim(), password }).subscribe({
            next: (sesion) => {
                this.router
                    .navigateByUrl(this.auth.rutaInicial(sesion.rol))
                    .then(() => {
                        // Si la página de inicio no existe aún (ej. /admin/panel del Frontend 2),
                        // la ruta comodín nos devuelve al login: avisamos en vez de dejar el botón girando.
                        if (this.router.url.startsWith('/login')) {
                            this.error.set('Tu página de inicio todavía no está disponible');
                            this.cargando.set(false);
                        }
                    })
                    .catch(() => {
                        this.error.set('No se pudo abrir tu página de inicio');
                        this.cargando.set(false);
                    });
            },
            error: (err: HttpErrorResponse) => {
                this.error.set(this.mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    /** Traduce la respuesta del backend a un mensaje para el usuario. */
    private mensajeDeError(err: HttpErrorResponse): string {
        if (err.status === 401 || err.status === 403) return 'Correo o contraseña incorrectos';
        if (err.status === 400) return 'Revisa el correo y la contraseña';
        // 0 = no hubo respuesta; 5xx = falla del servidor (el proxy de ng serve responde 500 si el backend está apagado)
        if (err.status === 0 || err.status >= 500) return 'El servidor no respondió. ¿Está corriendo el backend?';
        return 'Ocurrió un error al iniciar sesión. Intenta de nuevo';
    }
}