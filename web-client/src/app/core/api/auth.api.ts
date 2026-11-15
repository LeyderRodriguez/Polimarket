import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Perfil, SolicitudLogin } from './tipos';

/**
 * Llamadas HTTP de sesión. Solo habla con la API: no guarda estado ni decide nada (eso es de `SesionServicio`).
 * Las rutas son relativas para que `ng serve` las envíe al backend por el proxy y la cookie y el token CSRF
 * funcionen sin configuración extra.
 */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  login(solicitud: SolicitudLogin) {
    return this.http.post<Perfil>('/api/auth/login', solicitud);
  }

  me() {
    return this.http.get<Perfil>('/api/auth/me');
  }

  logout() {
    return this.http.post<void>('/api/auth/logout', null);
  }
}
