import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Cliente, SolicitudCliente } from './tipos';

/** Clientes de los vendedores: un tercero que además tiene el rol de cliente y sus preferencias. */
@Injectable({ providedIn: 'root' })
export class ClientesApi {
  private readonly http = inject(HttpClient);

  /** `GET /api/clientes` */
  listar() {
    return this.http.get<Cliente[]>('/api/clientes');
  }

  /** `POST /api/clientes`: convierte un tercero en cliente (`409 CLIENTE_EXISTENTE` si ya lo era). */
  registrar(solicitud: SolicitudCliente) {
    return this.http.post<Cliente>('/api/clientes', solicitud);
  }
}
