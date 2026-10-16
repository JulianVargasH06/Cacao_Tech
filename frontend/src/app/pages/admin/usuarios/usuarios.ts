import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';

import { Rol } from '../../../core/models/auth.model';
import { Usuario } from '../../../core/models/usuario.model';
import { AuthService } from '../../../core/services/auth.service';
import { UsuariosService } from '../../../core/services/usuarios.service';
import { PageHeader } from '../../../shared/page-header/page-header';
import { mensajeDeError } from '../../../shared/utils/errores-api';

/**
 * Gestión de cuentas internas (HU14): crear usuarios, asignar su rol y activarlos o desactivarlos.
 * Un usuario desactivado no puede iniciar sesión y su token deja de funcionar de inmediato.
 */
@Component({
    selector: 'app-usuarios',
    imports: [
        FormsModule,
        ReactiveFormsModule,
        DatePipe,
        PageHeader,
        CardModule,
        TableModule,
        TagModule,
        ButtonModule,
        SelectModule,
        DialogModule,
        InputTextModule,
        PasswordModule,
        MessageModule,
        ToastModule
    ],
    providers: [MessageService],
    template: `
    <section class="page">
      <p-toast />
      <app-page-header
        titulo="Usuarios"
        descripcion="Cuentas internas: quién puede entrar al sistema y con qué rol"
        historia="HU14"
        rol="Administrador"
      />

      @if (error()) {
        <p-message severity="error">
          <div class="mensaje-accion">
            <span>{{ error() }}</span>
            <p-button label="Reintentar" icon="pi pi-refresh" size="small" [text]="true" (onClick)="cargar()" />
          </div>
        </p-message>
      }

      <p-card>
        <p-table [value]="usuarios()" [loading]="cargando()" [tableStyle]="{ 'min-width': '52rem' }">
          <ng-template #caption>
            <div class="barra">
              <span class="total">{{ activos() }} activos de {{ usuarios().length }} usuarios</span>
              <p-button label="Nuevo usuario" icon="pi pi-user-plus" (onClick)="abrirNuevo()" />
            </div>
          </ng-template>

          <ng-template #header>
            <tr>
              <th>Nombre</th>
              <th>Correo</th>
              <th>Rol</th>
              <th>Estado</th>
              <th>Creado</th>
              <th></th>
            </tr>
          </ng-template>

          <ng-template #body let-u>
            <tr [class.inactivo]="!u.activo">
              <td>
                <strong>{{ u.nombre }}</strong>
                @if (esYo(u)) {
                  <p-tag value="Tú" severity="info" [rounded]="true" class="yo" />
                }
              </td>
              <td>{{ u.correo }}</td>
              <td>
                <p-select
                  [options]="roles"
                  [ngModel]="u.rol"
                  (ngModelChange)="cambiarRol(u, $event)"
                  optionLabel="etiqueta"
                  optionValue="valor"
                  [disabled]="esYo(u) || guardando() === u.id"
                  size="small"
                />
              </td>
              <td>
                <p-tag [value]="u.activo ? 'Activo' : 'Inactivo'" [severity]="u.activo ? 'success' : 'secondary'" [rounded]="true" />
              </td>
              <td class="sin-salto">{{ u.creadoEn | date: 'dd/MM/yyyy' }}</td>
              <td class="sin-salto">
                @if (!esYo(u)) {
                  <p-button
                    [label]="u.activo ? 'Desactivar' : 'Activar'"
                    [icon]="u.activo ? 'pi pi-user-minus' : 'pi pi-user-plus'"
                    [severity]="u.activo ? 'danger' : 'success'"
                    [outlined]="true"
                    size="small"
                    [loading]="guardando() === u.id"
                    (onClick)="cambiarActivo(u)"
                  />
                }
              </td>
            </tr>
          </ng-template>
        </p-table>
      </p-card>

      <!-- Crear usuario -->
      <p-dialog header="Nuevo usuario" [(visible)]="dialogoNuevo" [modal]="true" [style]="{ width: 'min(28rem, 95vw)' }">
        <form [formGroup]="form" (ngSubmit)="crear()" class="formulario">
          <label for="nombre">Nombre</label>
          <input pInputText id="nombre" formControlName="nombre" placeholder="Nombre y apellido" />
          @if (tieneError('nombre', 'required')) {
            <small class="error">El nombre es obligatorio</small>
          }

          <label for="correo">Correo</label>
          <input pInputText id="correo" type="email" formControlName="correo" placeholder="nombre@cacaotech.com" />
          @if (tieneError('correo', 'required')) {
            <small class="error">El correo es obligatorio</small>
          }
          @if (tieneError('correo', 'email')) {
            <small class="error">Escribe un correo válido</small>
          }
          @if (tieneError('correo', 'duplicado')) {
            <small class="error">Ya existe un usuario con este correo</small>
          }

          <label for="password">Contraseña inicial</label>
          <p-password inputId="password" formControlName="password" [toggleMask]="true" [feedback]="false"
            [fluid]="true" autocomplete="new-password" placeholder="Mínimo 8 caracteres" />
          @if (tieneError('password', 'required')) {
            <small class="error">La contraseña es obligatoria</small>
          }
          @if (tieneError('password', 'minlength') || tieneError('password', 'maxlength')) {
            <small class="error">Debe tener entre 8 y 72 caracteres</small>
          }

          <label for="rol">Rol</label>
          <p-select inputId="rol" formControlName="rol" [options]="roles" optionLabel="etiqueta" optionValue="valor" [fluid]="true" />

          <div class="acciones">
            <p-button label="Cancelar" severity="secondary" [text]="true" (onClick)="dialogoNuevo = false" />
            <p-button type="submit" label="Crear usuario" icon="pi pi-check" [loading]="creando()" />
          </div>
        </form>
      </p-dialog>
    </section>
  `,
    styles: `
    .barra {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .total {
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
    }
    .yo {
      margin-left: 0.5rem;
    }
    .inactivo td {
      opacity: 0.6;
    }
    .sin-salto {
      white-space: nowrap;
    }
    .formulario {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    .formulario label {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--p-primary-900);
      margin-top: 0.4rem;
    }
    .formulario input {
      width: 100%;
    }
    .error {
      color: var(--p-red-500);
    }
    .acciones {
      display: flex;
      justify-content: flex-end;
      gap: 0.5rem;
      margin-top: 1rem;
    }
    .mensaje-accion {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    :host ::ng-deep .p-datatable-header,
    :host ::ng-deep .p-datatable-thead > tr > th,
    :host ::ng-deep .p-datatable-tbody > tr {
      background: transparent;
    }
  `
})
export class Usuarios implements OnInit {
    private readonly usuariosService = inject(UsuariosService);
    private readonly auth = inject(AuthService);
    private readonly mensajes = inject(MessageService);

    protected readonly roles: { valor: Rol; etiqueta: string }[] = [
        { valor: 'OPERADOR', etiqueta: 'Operador' },
        { valor: 'ADMINISTRADOR', etiqueta: 'Administrador' }
    ];

    protected readonly usuarios = signal<Usuario[]>([]);
    protected readonly cargando = signal(true);
    protected readonly error = signal('');
    protected readonly guardando = signal<number | null>(null);
    protected readonly creando = signal(false);
    protected dialogoNuevo = false;

    protected readonly form = inject(FormBuilder).nonNullable.group({
        nombre: ['', [Validators.required, Validators.maxLength(100)]],
        correo: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
        rol: ['OPERADOR' as Rol, Validators.required]
    });

    ngOnInit(): void {
        this.cargar();
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set('');
        this.usuariosService.listar().subscribe({
            next: (usuarios) => {
                this.usuarios.set(usuarios);
                this.cargando.set(false);
            },
            error: (err: HttpErrorResponse) => {
                this.error.set('No se pudieron cargar los usuarios: ' + mensajeDeError(err));
                this.cargando.set(false);
            }
        });
    }

    protected activos(): number {
        return this.usuarios().filter((u) => u.activo).length;
    }

    /** El administrador no puede desactivarse ni cambiarse el rol a sí mismo (el backend también lo impide). */
    protected esYo(u: Usuario): boolean {
        const yo = this.auth.correo();
        return !!yo && u.correo.toLowerCase() === yo.toLowerCase();
    }

    protected tieneError(campo: string, error: string): boolean {
        const control = this.form.get(campo);
        return !!control && control.touched && control.hasError(error);
    }

    protected abrirNuevo(): void {
        this.form.reset({ nombre: '', correo: '', password: '', rol: 'OPERADOR' });
        this.dialogoNuevo = true;
    }

    protected crear(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }
        const datos = this.form.getRawValue();
        this.creando.set(true);
        this.usuariosService
            .crear({ ...datos, nombre: datos.nombre.trim(), correo: datos.correo.trim().toLowerCase() })
            .subscribe({
                next: (usuario) => {
                    this.creando.set(false);
                    this.dialogoNuevo = false;
                    this.usuarios.update((lista) => [...lista, usuario].sort((a, b) => a.nombre.localeCompare(b.nombre)));
                    this.mensajes.add({ severity: 'success', summary: 'Usuario creado', detail: `${usuario.nombre} ya puede iniciar sesión` });
                },
                error: (err: HttpErrorResponse) => {
                    this.creando.set(false);
                    if (/ya existe un usuario/i.test(err.error?.error ?? '')) {
                        this.form.controls.correo.setErrors({ duplicado: true });
                        return;
                    }
                    this.mensajes.add({ severity: 'error', summary: 'No se pudo crear', detail: mensajeDeError(err) });
                }
            });
    }

    protected cambiarRol(u: Usuario, rol: Rol): void {
        if (rol === u.rol) return;
        this.actualizar(u, { rol }, `${u.nombre} ahora es ${rol === 'ADMINISTRADOR' ? 'administrador' : 'operador'}`);
    }

    protected cambiarActivo(u: Usuario): void {
        this.actualizar(
            u,
            { activo: !u.activo },
            u.activo ? `${u.nombre} ya no puede iniciar sesión` : `${u.nombre} puede volver a iniciar sesión`
        );
    }

    private actualizar(u: Usuario, cambios: { rol?: Rol; activo?: boolean }, detalle: string): void {
        this.guardando.set(u.id);
        this.usuariosService.actualizar(u.id, cambios).subscribe({
            next: (actualizado) => {
                this.guardando.set(null);
                this.usuarios.update((lista) => lista.map((x) => (x.id === actualizado.id ? actualizado : x)));
                this.mensajes.add({ severity: 'success', summary: 'Usuario actualizado', detail: detalle });
            },
            error: (err: HttpErrorResponse) => {
                this.guardando.set(null);
                // Se vuelve a pintar la fila con los valores reales (el select pudo quedar con el rol rechazado)
                this.usuarios.update((lista) => lista.map((x) => (x.id === u.id ? { ...u } : x)));
                this.mensajes.add({ severity: 'error', summary: 'No se pudo actualizar', detail: mensajeDeError(err) });
            }
        });
    }
}
