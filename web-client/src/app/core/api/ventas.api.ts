import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { SolicitudVenta, Venta } from './tipos';

/** Ventas. El total, el aval y el stock los decide el backend; aquí solo se envía y se recibe. */
@Injectable({ providedIn: 'root' })
export class VentasApi {
  private readonly http = inject(HttpClient);

  /** `POST /api/ventas`: registra la venta y reserva el stock, todo o nada. */
  registrar(solicitud: SolicitudVenta) {
    return this.http.post<Venta>('/api/ventas', solicitud);
  }
}
