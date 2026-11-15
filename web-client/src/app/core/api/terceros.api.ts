import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { SolicitudTercero, Tercero } from './tipos';

/** Terceros: los datos comunes de personas y empresas, que se registran una sola vez. */
@Injectable({ providedIn: 'root' })
export class TercerosApi {
  private readonly http = inject(HttpClient);

  /** `GET /api/terceros`: lo encuentra por documento o responde `404 NO_ENCONTRADO`. */
  buscar(tipoDocumento: string, numeroDocumento: string) {
    return this.http.get<Tercero>('/api/terceros', { params: { tipoDocumento, numeroDocumento } });
  }

  /** `POST /api/terceros`: lo registra (`409 DOCUMENTO_DUPLICADO` si ya existe). */
  registrar(solicitud: SolicitudTercero) {
    return this.http.post<Tercero>('/api/terceros', solicitud);
  }
}
