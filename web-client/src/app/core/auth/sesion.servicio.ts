import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, timeout } from 'rxjs';
import { AuthApi } from '../api/auth.api';
import type { ErrorApi, Perfil, Rol } from '../api/tipos';
import { aErrorApi } from '../http/error-api';

/** Roles que usan este cliente. Recursos Humanos y Bodega trabajan con el cliente de escritorio. */
const ROLES_WEB: readonly Rol[] = ['VENDEDOR', 'ENTREGAS'];

/** Cuánto se espera al backend al abrir la página antes de rendirse; sin esto una respuesta que nunca llega dejaría la pantalla en blanco. */
const ESPERA_MAXIMA_AL_RESTAURAR_MS = 5000;

/** Quién está dentro. Es el único lugar que guarda la sesión; el backend sigue siendo quien la valida. */
@Injectable({ providedIn: 'root' })
export class SesionServicio {
  private readonly api = inject(AuthApi);
  private readonly router = inject(Router);
  private readonly actual = signal<Perfil | null>(null);

  private readonly fallo = signal<ErrorApi | null>(null);

  readonly usuario = this.actual.asReadonly();
  readonly autenticado = computed(() => this.actual() !== null);
  /** Por qué no se pudo comprobar la sesión al abrir la página (backend caído o sin respuesta); `null` si no hubo problema. */
  readonly errorRestauracion = this.fallo.asReadonly();

  /**
   * Al arrancar: si la cookie de sesión sigue viva, recupera quién es; si no, queda sin sesión. «No hay sesión» (401) es
   * lo normal y no es un error; si el servidor no responde, queda sin sesión pero con el motivo en `errorRestauracion`.
   */
  async restaurar(): Promise<void> {
    this.fallo.set(null);
    try {
      this.actual.set(await firstValueFrom(this.api.me().pipe(timeout(ESPERA_MAXIMA_AL_RESTAURAR_MS))));
    } catch (error) {
      this.actual.set(null);
      const detalle = aErrorApi(error);
      if (detalle.estado !== 401) {
        this.fallo.set(detalle);
      }
    }
  }

  async iniciarSesion(correo: string, contrasena: string): Promise<Perfil> {
    this.fallo.set(null);
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
