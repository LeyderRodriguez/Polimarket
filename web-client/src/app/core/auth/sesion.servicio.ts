import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthApi } from '../api/auth.api';
import type { ErrorApi, Perfil, Rol } from '../api/tipos';

/** Roles que usan este cliente. Recursos Humanos y Bodega trabajan con el cliente de escritorio. */
const ROLES_WEB: readonly Rol[] = ['VENDEDOR', 'ENTREGAS'];

/** Quién está dentro. Es el único lugar que guarda la sesión; el backend sigue siendo quien la valida. */
@Injectable({ providedIn: 'root' })
export class SesionServicio {
  private readonly api = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly actual = signal<Perfil | null>(null);

  readonly usuario = this.actual.asReadonly();
  readonly autenticado = computed(() => this.actual() !== null);

  /** Al arrancar: si la cookie de sesión sigue viva, recupera quién es; si no, queda sin sesión. */
  async restaurar(): Promise<void> {
    try {
      this.actual.set(await firstValueFrom(this.api.me()));
    } catch {
      this.actual.set(null);
    }
  }

  async iniciarSesion(correo: string, contrasena: string): Promise<Perfil> {
    const perfil = await firstValueFrom(this.api.login({ correo, contrasena }));
    if (!ROLES_WEB.includes(perfil.rol)) {
      // La cuenta es válida pero no es para esta aplicación: se cierra la sesión que acaba de abrir el backend.
      await firstValueFrom(this.api.logout()).catch(() => undefined);
      const rechazo: ErrorApi = {
        estado: 403,
        codigo: 'ROL_NO_SOPORTADO',
        mensaje: 'Esta aplicación es para vendedores y personal de entregas. Recursos Humanos y Bodega usan el cliente de escritorio.',
      };
      throw rechazo;
    }
    this.actual.set(perfil);
    return perfil;
  }

  async cerrarSesion(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout());
    } catch {
      // Si el servidor no responde, igual se limpia la sesión local.
    }
    this.limpiar();
    await this.router.navigateByUrl('/login');
  }

  /** Olvida al usuario sin avisar al servidor (la sesión ya caducó allá). */
  limpiar(): void {
    this.actual.set(null);
  }
}
